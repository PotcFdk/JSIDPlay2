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
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "sidplay.consoleplayer.ConsolePlayer")
public class ConsolePlayer {
	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = "--cpuDebug", hidden = true, descriptionKey = "DEBUG")
	private Boolean cpuDebug = Boolean.FALSE;

	@Parameter(names = "-audio", descriptionKey = "DRIVER", converter = OutputConverter.class)
	private Audio audio = Audio.SOUNDCARD;

	@Parameter(names = "-emulation", descriptionKey = "EMULATION", converter = EmulationConverter.class)
	private Emulation emulation = Emulation.RESID;

	@Parameter(names = "-outputfile", descriptionKey = "OUTPUTFILE")
	private String outputFile = "outfile.wav";

	@Parameter(names = "-startSong", descriptionKey = "START_SONG")
	private Integer song = 0;

	@Parameter(names = "-loop", descriptionKey = "LOOP")
	private Boolean loop = Boolean.FALSE;

	@Parameter(names = "-single", descriptionKey = "SINGLE")
	private Boolean single = Boolean.FALSE;

	@Parameter(names = "-frequency", descriptionKey = "FREQUENCY")
	private Integer frequency = 48000;

	@Parameter(names = "-dualSID", descriptionKey = "DUAL_SID")
	private Boolean dualSID = Boolean.FALSE;

	@Parameter(names = "-forceClock", descriptionKey = "FORCE_CLOCK", converter = CPUClockConverter.class)
	private CPUClock forceClock = null;

	@Parameter(names = "-defaultClock", descriptionKey = "DEFAULT_CLOCK", converter = CPUClockConverter.class)
	private CPUClock defaultClock = CPUClock.PAL;

	@Parameter(names = "-disableFilter", descriptionKey = "DISABLE_FILTER")
	private Boolean disableFilter = Boolean.FALSE;

	@Parameter(names = "-forceModel", descriptionKey = "FORCE_MODEL", converter = ChipModelConverter.class)
	private ChipModel forceModel = null;

	@Parameter(names = "-defaultModel", descriptionKey = "DEFAULT_MODEL", converter = ChipModelConverter.class)
	private ChipModel defaultModel = ChipModel.MOS6581;

	@Parameter(names = "-startTime", descriptionKey = "START_TIME", converter = TimeConverter.class)
	private Integer startTime = 0;

	@Parameter(names = "-fixedLength", descriptionKey = "FIXED_LENGTH", converter = TimeConverter.class)
	private Integer fixedLength = 0;

	@Parameter(names = "-verbose", descriptionKey = "VERBOSE", validateWith = VerboseValidator.class)
	private Integer verbose = 0;

	@Parameter(names = "-quiet", descriptionKey = "QUIET")
	private Boolean quiet = Boolean.FALSE;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	private ConsolePlayer(String[] args) {
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
			player.getTune().setOutputFilename(outputFile);
		} catch (IOException | SidTuneError e) {
			e.getMessage();
			exit(1);
		}
		player.setDebug(cpuDebug);
		player.setDriverSettings(new DriverSettings(audio, emulation));
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
		ConsoleIO consoleIO = new ConsoleIO(config, quiet, verbose);
		player.setMenuHook(obj -> consoleIO.menu(obj, System.out));
		player.setInteractivityHook(obj -> consoleIO.decodeKeys(obj));

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

	private boolean isRecording() {
		return audio == Audio.WAV || audio == Audio.MP3
				|| audio == Audio.LIVE_WAV || audio == Audio.LIVE_MP3;
	}

	private void exit(int rc) {
		System.out.println("Press <enter> to exit the player!");
		try {
			System.in.read();
			System.exit(rc);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void main(final String[] args) {
		new ConsolePlayer(args);
	}

}
