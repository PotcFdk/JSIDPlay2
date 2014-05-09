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
import libsidutils.cpuparser.CPUParser;
import resid_builder.resid.ChipModel;
import sidplay.audio.Output;
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

public class ConsolePlayer {
	@Parameter(names = { "--help", "-h" }, help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = "-driver", description = "Audio driver (OUT_NULL, OUT_SOUNDCARD, OUT_WAV, OUT_MP3, OUT_LIVE_WAV or OUT_LIVE_MP3)", converter = OutputConverter.class)
	private Output driver = Output.OUT_SOUNDCARD;

	@Parameter(names = "-emulation", description = "Emulation (EMU_NONE, EMU_RESID, EMU_HARDSID)", converter = EmulationConverter.class)
	private Emulation emulation = Emulation.EMU_RESID;

	@Parameter(names = "-output", description = "Output filename for recording")
	private String output = "outfile.wav";

	@Parameter(names = "-o", description = "start track (default: tune start song)")
	private Integer songNum = 0;

	@Parameter(names = "-l", description = "Loop track")
	private Boolean loop = Boolean.FALSE;

	@Parameter(names = "-s", description = "Single track")
	private Boolean single = Boolean.FALSE;

	@Parameter(names = "-f", description = "set frequency in Hz")
	private Integer frequency = 48000;

	@Parameter(names = "-fd", description = "Force dual sid environment")
	private Boolean forceStereoTune = Boolean.FALSE;

	@Parameter(names = "-vf", description = "Force user specific VIC PAL/NTSC clock speed (default: defined by song)", converter = CPUClockConverter.class)
	private CPUClock userClock = null;

	@Parameter(names = "-vd", description = "Set default VIC PAL/NTSC clock speed (to be used, if UNKNOWN)", converter = CPUClockConverter.class)
	private CPUClock defaultClock = CPUClock.PAL;

	@Parameter(names = "-nf", description = "No SID filter emulation")
	private Boolean disableFilters = Boolean.FALSE;

	@Parameter(names = "-nu", description = "Use waveforms MOS8580 or MOS6581 (default: from tune or cfg)", converter = ChipModelConverter.class)
	private ChipModel userModel = null;

	@Parameter(names = "-nd", description = "Default waveforms MOS8580 or MOS6581 (to be used, if UNKNOWN)", converter = ChipModelConverter.class)
	private ChipModel defaultModel = ChipModel.MOS6581;

	@Parameter(names = "-b", description = "Start time in [m:]s format", converter = TimeConverter.class)
	private Integer startTime = 0;

	@Parameter(names = "-t", description = "Set fixed play length in [m:]s format (0 is endless)", converter = TimeConverter.class)
	private Integer userPlayLength = 0;

	@Parameter(names = "-v", description = "Verbose (level=0,1,2)", validateWith = VerboseValidator.class)
	private Integer verboseLevel = 0;

	@Parameter(names = "-q", description = "Quiet (no output)")
	private Boolean quiet = Boolean.FALSE;

	@Parameter(names = "--cpu-debug", hidden = true, description = "Display cpu register and assembly dumps")
	private Boolean debug = Boolean.FALSE;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	public ConsolePlayer(String[] args) throws IOException {
		JCommander commander = new JCommander(this, args);
		if (help || filenames.size() != 1) {
			commander.usage();
			exit(help ? 0 : 1);
		}
		// Can only loop if not creating audio files
		if (isRecording()) {
			loop = false;
			single = true;
		}
		final IniConfig config = new IniConfig(true);
		config.getSidplay2().setLoop(loop);
		config.getSidplay2().setSingle(single);
		config.getSidplay2().setUserPlayLength(userPlayLength);
		config.getAudio().setFrequency(frequency);
		config.getEmulation().setForceStereoTune(forceStereoTune);
		config.getEmulation().setUserClockSpeed(userClock);
		config.getEmulation().setDefaultClockSpeed(defaultClock);
		config.getEmulation().setUserSidModel(userModel);
		config.getEmulation().setDefaultSidModel(defaultModel);
		config.getEmulation().setFilter(!disableFilters);

		final Player player = new Player(config);
		try {
			player.setTune(SidTune.load(new File(filenames.get(0))));
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
			exit(1);
		}
		player.setDebug(debug ? CPUParser.getInstance() : null);
		player.setDriverSettings(new DriverSettings(driver, emulation));
		player.getTune().setOutputFilename(output);
		// Select the desired track and also mark the play-list start
		player.getTrack().setSelected(player.getTune().selectSong(songNum));
		player.getTrack().setFirst(0);
		player.getTrack().setSongs(
				config.getSidplay2().isSingle() ? 1 : player.getTune()
						.getInfo().songs);
		player.getTimer().setStart(startTime);

		// check song length
		if (config.getSidplay2().getUserPlayLength() == 0) {
			String hvscRoot = config.getSidplay2().getHvsc();
			if (hvscRoot != null) {
				File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
				try (FileInputStream input = new FileInputStream(file)) {
					player.setSidDatabase(new SidDatabase(input));
				} catch (IOException e) {
					// silently ignored!
				}
			}
			if (isRecording()
					&& (!config.getSidplay2().isEnableDatabase() || player
							.getSongLength(player.getTune()) == 0)) {
				System.err
						.println("ERROR: unknown song length in record mode"
								+ " (please use option -t or configure song length database)");
				exit(1);
			}
		}
		ConsoleIO consoleIO = new ConsoleIO(config, player, quiet, verboseLevel);
		player.setMenuHook((obj) -> consoleIO.menu(player.getTune(),
				player.getTrack(), player.getTimer()));
		player.setInteractivityHook((obj) -> consoleIO.decodeKeys());

		player.startC64();
	}

	private void exit(int rc) throws IOException {
		System.out.println("Press <enter> to exit!");
		System.in.read();
		System.exit(rc);
	}

	private boolean isRecording() {
		return driver == Output.OUT_WAV || driver == Output.OUT_MP3
				|| driver == Output.OUT_LIVE_WAV
				|| driver == Output.OUT_LIVE_MP3;
	}

	public static void main(final String[] args) throws IOException {
		new ConsolePlayer(args);
	}

}
