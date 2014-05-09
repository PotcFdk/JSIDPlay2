package sidplay;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.player.DriverSettings;
import libsidplay.player.Emulation;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.SidDatabase;
import resid_builder.resid.ChipModel;
import sidplay.audio.Audio;
import sidplay.consoleplayer.CPUClockConverter;
import sidplay.consoleplayer.ChipModelConverter;
import sidplay.consoleplayer.ConsoleIO;
import sidplay.consoleplayer.EmulationConverter;
import sidplay.consoleplayer.OutputConverter;
import sidplay.consoleplayer.TimeConverter;
import sidplay.consoleplayer.VerboseValidator;
import sidplay.ini.IniConfig;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class ConsolePlayer {
	@Parameter(names = { "--help", "-h" }, description = "Display usage", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = "--cpuDebug", hidden = true, description = "Display cpu register and assembly dumps")
	private Boolean cpuDebug = Boolean.FALSE;

	@Parameter(names = "-driver", description = "Audio driver (NONE, SOUNDCARD, WAV, MP3, LIVE_WAV or LIVE_MP3)", converter = OutputConverter.class)
	private Audio driver = Audio.SOUNDCARD;

	@Parameter(names = "-emulation", description = "Emulation (NONE, RESID, HARDSID)", converter = EmulationConverter.class)
	private Emulation emulation = Emulation.RESID;

	@Parameter(names = "-output", description = "Output filename for recording")
	private String output = "outfile.wav";

	@Parameter(names = "-song", description = "Start track (default: tune start song)")
	private Integer song = 0;

	@Parameter(names = "-loop", description = "Loop track")
	private Boolean loop = Boolean.FALSE;

	@Parameter(names = "-single", description = "Single track")
	private Boolean single = Boolean.FALSE;

	@Parameter(names = "-frequency", description = "Set frequency in Hz")
	private Integer frequency = 48000;

	@Parameter(names = "-dualSID", description = "Force dual sid environment")
	private Boolean dualSID = Boolean.FALSE;

	@Parameter(names = "-forceClock", description = "Force user specific VIC clock speed PAL or NTSC (default: defined by song)", converter = CPUClockConverter.class)
	private CPUClock forceClock = null;

	@Parameter(names = "-defaultClock", description = "Set default VIC clock speed PAL or NTSC (to be used, if UNKNOWN)", converter = CPUClockConverter.class)
	private CPUClock defaultClock = CPUClock.PAL;

	@Parameter(names = "-disableFilter", description = "No SID filter emulation")
	private Boolean disableFilter = Boolean.FALSE;

	@Parameter(names = "-forceModel", description = "Use waveforms MOS8580 or MOS6581 (default: from tune or cfg)", converter = ChipModelConverter.class)
	private ChipModel forceModel = null;

	@Parameter(names = "-defaultModel", description = "Default waveforms MOS8580 or MOS6581 (to be used, if UNKNOWN)", converter = ChipModelConverter.class)
	private ChipModel defaultModel = ChipModel.MOS6581;

	@Parameter(names = "-startTime", description = "Start time in [m:]s format", converter = TimeConverter.class)
	private Integer startTime = 0;

	@Parameter(names = "-fixedLength", description = "Set fixed play length in [m:]s format (0 is endless)", converter = TimeConverter.class)
	private Integer fixedLength = 0;

	@Parameter(names = "-verbose", description = "Verbose (level=0,1,2)", validateWith = VerboseValidator.class)
	private Integer verbose = 0;

	@Parameter(names = "-quiet", description = "Quiet (no output)")
	private Boolean quiet = Boolean.FALSE;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	private ConsolePlayer(String[] args) throws IOException {
		try {
			JCommander commander = new JCommander(this, args);
			commander.setProgramName(getClass().getName());
			commander.setCaseSensitiveOptions(true);
			if (help || filenames.size() != 1) {
				commander.usage();
				exit(help ? 0 : 1);
			}
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			exit(1);
		}
		// Can only loop if not creating audio files
		if (isRecording()) {
			loop = false;
			single = true;
		}
		final IniConfig config = new IniConfig(true);
		config.getSidplay2().setLoop(loop);
		config.getSidplay2().setSingle(single);
		config.getSidplay2().setUserPlayLength(fixedLength);
		config.getAudio().setFrequency(frequency);
		config.getEmulation().setForceStereoTune(dualSID);
		config.getEmulation().setUserClockSpeed(forceClock);
		config.getEmulation().setDefaultClockSpeed(defaultClock);
		config.getEmulation().setUserSidModel(forceModel);
		config.getEmulation().setDefaultSidModel(defaultModel);
		config.getEmulation().setFilter(!disableFilter);

		final Player player = new Player(config);
		try {
			player.setTune(SidTune.load(new File(filenames.get(0))));
			player.getTune().setOutputFilename(output);
		} catch (IOException | SidTuneError e) {
			e.getMessage();
			exit(1);
		}
		player.setDebug(cpuDebug);
		player.setDriverSettings(new DriverSettings(driver, emulation));
		// Select the desired track and also mark the play-list start
		player.getTrack().setSelected(player.getTune().selectSong(song));
		player.getTrack().setFirst(0);
		player.getTrack().setSongs(
				config.getSidplay2().isSingle() ? 1 : player.getTune()
						.getInfo().songs);
		player.getTimer().setStart(startTime);

		// check song length
		if (config.getSidplay2().getUserPlayLength() == 0) {
			setSIDDatabase(player);
			if (isRecording()
					&& (!config.getSidplay2().isEnableDatabase() || player
							.getSongLength(player.getTune()) == 0)) {
				System.err
						.println("ERROR: unknown song length in record mode"
								+ " (please use option -t or configure song length database)");
				exit(1);
			}
		}
		ConsoleIO consoleIO = new ConsoleIO(config, player, quiet, verbose);
		player.setMenuHook(obj -> consoleIO.menu(player.getTune(),
				player.getTrack(), player.getTimer()));
		player.setInteractivityHook(obj -> consoleIO.decodeKeys());

		player.startC64();
	}

	private void setSIDDatabase(final Player player) {
		String hvscRoot = player.getConfig().getSidplay2().getHvsc();
		if (hvscRoot != null) {
			File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
			try (FileInputStream input = new FileInputStream(file)) {
				player.setSidDatabase(new SidDatabase(input));
			} catch (IOException e) {
				// silently ignored!
			}
		}
	}

	private void exit(int rc) throws IOException {
		System.out.println("Press <enter> to exit!");
		System.in.read();
		System.exit(rc);
	}

	private boolean isRecording() {
		return driver == Audio.WAV || driver == Audio.MP3
				|| driver == Audio.LIVE_WAV || driver == Audio.LIVE_MP3;
	}

	public static void main(final String[] args) throws IOException {
		new ConsolePlayer(args);
	}

}
