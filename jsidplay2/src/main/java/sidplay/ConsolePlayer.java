package sidplay;

import static libsidplay.config.IAudioSection.DEFAULT_AUDIO;
import static libsidplay.config.IAudioSection.DEFAULT_BUFFER_SIZE;
import static libsidplay.config.IAudioSection.DEFAULT_SAMPLING_RATE;
import static libsidplay.config.IAudioSection.DEFAULT_DEVICE;
import static libsidplay.config.IEmulationSection.DEFAULT_CLOCK_SPEED;
import static libsidplay.config.IEmulationSection.DEFAULT_EMULATION;
import static libsidplay.config.IEmulationSection.DEFAULT_ENGINE;
import static libsidplay.config.IEmulationSection.DEFAULT_FORCE_3SID_TUNE;
import static libsidplay.config.IEmulationSection.DEFAULT_FORCE_STEREO_TUNE;
import static libsidplay.config.IEmulationSection.DEFAULT_SID_MODEL;
import static libsidplay.config.IEmulationSection.DEFAULT_USE_3SID_FILTER;
import static libsidplay.config.IEmulationSection.DEFAULT_USE_FILTER;
import static libsidplay.config.IEmulationSection.DEFAULT_USE_STEREO_FILTER;
import static libsidplay.config.ISidPlay2Section.DEFAULT_ENABLE_DATABASE;
import static libsidplay.config.ISidPlay2Section.DEFAULT_LOOP;
import static libsidplay.config.ISidPlay2Section.DEFAULT_PLAY_LENGTH;
import static libsidplay.config.ISidPlay2Section.DEFAULT_SINGLE_TRACK;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SamplingRate;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.debug.MOS6510Debug;
import libsidutils.siddatabase.SidDatabase;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.audio.JavaSound.Device;
import sidplay.consoleplayer.ConsoleIO;
import sidplay.consoleplayer.TimeConverter;
import sidplay.consoleplayer.VerboseValidator;
import sidplay.ini.IniConfig;

@Parameters(resourceBundle = "sidplay.consoleplayer.ConsolePlayer")
final public class ConsolePlayer {
	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = "--cpuDebug", hidden = true, descriptionKey = "DEBUG")
	private Boolean cpuDebug = Boolean.FALSE;

	@Parameter(names = { "--audio", "-a" }, descriptionKey = "DRIVER")
	private Audio audio = DEFAULT_AUDIO;

	@Parameter(names = { "--bufferSize", "-B" }, descriptionKey = "BUFFER_SIZE")
	private int bufferSize = DEFAULT_BUFFER_SIZE;

	@Parameter(names = { "--deviceIndex", "-A" }, descriptionKey = "DEVICEINDEX")
	private Integer deviceIdx = DEFAULT_DEVICE;

	@Parameter(names = { "--engine", "-E" }, descriptionKey = "ENGINE")
	private Engine engine = DEFAULT_ENGINE;

	@Parameter(names = { "--defaultEmulation", "-e" }, descriptionKey = "DEFAULT_EMULATION")
	private Emulation defaultEmulation = DEFAULT_EMULATION;

	@Parameter(names = { "--recordingFilename", "-r" }, descriptionKey = "RECORDING_FILENAME")
	private String recordingFilename = "jsidplay2";

	@Parameter(names = { "--startSong", "-o" }, descriptionKey = "START_SONG")
	private Integer song = null;

	@Parameter(names = { "--loop", "-l" }, descriptionKey = "LOOP")
	private Boolean loop = DEFAULT_LOOP;

	@Parameter(names = { "--single", "-s" }, descriptionKey = "SINGLE")
	private Boolean single = DEFAULT_SINGLE_TRACK;

	@Parameter(names = { "--frequency", "-f" }, descriptionKey = "FREQUENCY")
	private SamplingRate samplingRate = DEFAULT_SAMPLING_RATE;

	@Parameter(names = { "--dualSID", "-d" }, descriptionKey = "DUAL_SID")
	private Boolean dualSID = DEFAULT_FORCE_STEREO_TUNE;

	@Parameter(names = { "--thirdSID", "-D" }, descriptionKey = "THIRD_SID")
	private Boolean thirdSID = DEFAULT_FORCE_3SID_TUNE;

	@Parameter(names = { "--forceClock", "-c" }, descriptionKey = "FORCE_CLOCK")
	private CPUClock forceClock = null;

	@Parameter(names = { "--defaultClock", "-k" }, descriptionKey = "DEFAULT_CLOCK")
	private CPUClock defaultClock = DEFAULT_CLOCK_SPEED;

	@Parameter(names = { "--disableFilter", "-i" }, descriptionKey = "DISABLE_FILTER")
	private Boolean disableFilter = !DEFAULT_USE_FILTER;

	@Parameter(names = { "--disableStereoFilter", "-j" }, descriptionKey = "DISABLE_STEREO_FILTER")
	private Boolean disableStereoFilter = !DEFAULT_USE_STEREO_FILTER;

	@Parameter(names = { "--disable3rdSidFilter", "-J" }, descriptionKey = "DISABLE_3RD_SID_FILTER")
	private Boolean disable3rdSIDFilter = !DEFAULT_USE_3SID_FILTER;

	@Parameter(names = { "--forceModel", "-m" }, descriptionKey = "FORCE_MODEL")
	private ChipModel forceModel = ChipModel.AUTO;

	@Parameter(names = { "--defaultModel", "-u" }, descriptionKey = "DEFAULT_MODEL")
	private ChipModel defaultModel = DEFAULT_SID_MODEL;

	@Parameter(names = { "--startTime", "-t" }, descriptionKey = "START_TIME", converter = TimeConverter.class)
	private Integer startTime = 0;

	@Parameter(names = { "--defaultLength", "-g" }, descriptionKey = "DEFAULT_LENGTH", converter = TimeConverter.class)
	private Integer defaultLength = DEFAULT_PLAY_LENGTH;

	@Parameter(names = { "--enableSidDatabase", "-n" }, descriptionKey = "ENABLE_SID_DATABASE", arity = 1)
	private Boolean enableSidDatabase = DEFAULT_ENABLE_DATABASE;

	@Parameter(names = { "--verbose", "-v" }, descriptionKey = "VERBOSE", validateWith = VerboseValidator.class)
	private Integer verbose = 0;

	@Parameter(names = { "--quiet", "-q" }, descriptionKey = "QUIET")
	private Boolean quiet = Boolean.FALSE;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	private ConsolePlayer(final String[] args) {
		try {
			JCommander commander = new JCommander(this, args);
			commander.setProgramName(getClass().getName());
			commander.setCaseSensitiveOptions(true);
			if (help || filenames.size() != 1) {
				commander.usage();
				printSoundcardDevices();
				exit(1);
			}
			// Cannot loop while recording audio files
			if (isRecording()) {
				loop = false;
			}
			final IniConfig config = new IniConfig(true);
			config.getSidplay2Section().setLoop(loop);
			config.getSidplay2Section().setSingle(single);
			config.getSidplay2Section().setDefaultPlayLength(defaultLength);
			config.getSidplay2Section().setEnableDatabase(enableSidDatabase);
			config.getAudioSection().setAudio(audio);
			config.getAudioSection().setSamplingRate(samplingRate);
			config.getAudioSection().setBufferSize(bufferSize);
			config.getAudioSection().setDevice(deviceIdx);
			config.getEmulationSection().setEngine(engine);
			config.getEmulationSection().setDefaultEmulation(defaultEmulation);
			config.getEmulationSection().setForceStereoTune(dualSID);
			config.getEmulationSection().setForce3SIDTune(thirdSID);
			config.getEmulationSection().setUserClockSpeed(forceClock);
			config.getEmulationSection().setDefaultClockSpeed(defaultClock);
			config.getEmulationSection().setUserSidModel(forceModel);
			config.getEmulationSection().setDefaultSidModel(defaultModel);
			config.getEmulationSection().setFilter(!disableFilter);
			config.getEmulationSection().setStereoFilter(!disableStereoFilter);
			config.getEmulationSection().setThirdSIDFilter(!disable3rdSIDFilter);

			String filename = filenames.get(0);
			final SidTune tune = SidTune.load(new File(filename));
			tune.getInfo().setSelectedSong(song);
			final Player player = new Player(config, cpuDebug ? MOS6510Debug.class : MOS6510.class);
			player.setTune(tune);
			player.getTimer().setStart(startTime);
			final ConsoleIO consoleIO = new ConsoleIO(config, filename);
			player.setMenuHook(obj -> consoleIO.menu(obj, verbose, quiet, System.out));
			player.setInteractivityHook(obj -> consoleIO.decodeKeys(obj));
			if (config.getSidplay2Section().isEnableDatabase()) {
				setSIDDatabase(player);
			}
			player.setRecordingFilenameProvider(theTune -> {
				File file = new File(recordingFilename);
				String basename = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()))
						.getAbsolutePath();
				if (theTune.getInfo().getSongs() > 1) {
					basename += String.format("-%02d", theTune.getInfo().getCurrentSong());
				}
				return basename;
			});
			if (isRecording() && defaultLength == 0
					&& player.getSidDatabaseInfo(db -> db.getSongLength(tune), 0) == 0) {
				System.err.println("ERROR: unknown song length in record mode"
						+ " (please use option --defaultLength or configure song length database)");
				exit(1);
			}
			player.startC64();
		} catch (ParameterException | IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			exit(1);
		}
	}

	private void setSIDDatabase(final Player player) {
		String hvscRoot = player.getConfig().getSidplay2Section().getHvsc();
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
		return audio == Audio.WAV || audio == Audio.MP3 || audio == Audio.LIVE_WAV || audio == Audio.LIVE_MP3;
	}

	private void printSoundcardDevices() {
		int deviceIdx = 0;
		for (Device device : JavaSound.getDevices()) {
			System.out.printf("    --deviceIndex %d -> %s (%s)\n", (deviceIdx++), device.getInfo().getName(),
					device.getInfo().getDescription());
		}
	}

	private void exit(int rc) {
		try {
			System.out.println("Press <enter> to exit the player!");
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
