package sidplay;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_ENABLE_DATABASE;
import static sidplay.ini.IniDefaults.DEFAULT_ENGINE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_3SID_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_STEREO_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_LOOP;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_LENGTH;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SINGLE_TRACK;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import sidplay.audio.MP3Driver;
import sidplay.audio.ProxyDriver;
import sidplay.consoleplayer.ConsoleIO;
import sidplay.consoleplayer.ParameterTimeConverter;
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

	@Parameter(names = { "--startTime", "-t" }, descriptionKey = "START_TIME", converter = ParameterTimeConverter.class)
	private Integer startTime = 0;

	@Parameter(names = { "--defaultLength", "-g" }, descriptionKey = "DEFAULT_LENGTH", converter = ParameterTimeConverter.class)
	private Integer defaultLength = DEFAULT_PLAY_LENGTH;

	@Parameter(names = { "--enableSidDatabase", "-n" }, descriptionKey = "ENABLE_SID_DATABASE", arity = 1)
	private Boolean enableSidDatabase = DEFAULT_ENABLE_DATABASE;

	@Parameter(names = { "--muteVoice1", "-1" }, descriptionKey = "MUTE_VOICE_1")
	private Boolean muteVoice1 = DEFAULT_MUTE_VOICE1;

	@Parameter(names = { "--muteVoice2", "-2" }, descriptionKey = "MUTE_VOICE_2")
	private Boolean muteVoice2 = DEFAULT_MUTE_VOICE2;

	@Parameter(names = { "--muteVoice3", "-3" }, descriptionKey = "MUTE_VOICE_3")
	private Boolean muteVoice3 = DEFAULT_MUTE_VOICE3;

	@Parameter(names = { "--muteVoice4", "-4" }, descriptionKey = "MUTE_VOICE_4")
	private Boolean muteVoice4 = DEFAULT_MUTE_VOICE4;

	@Parameter(names = { "--muteStereoVoice1", "-5" }, descriptionKey = "MUTE_VOICE_5")
	private Boolean muteVoice5 = DEFAULT_MUTE_STEREO_VOICE1;

	@Parameter(names = { "--muteStereoVoice2", "-6" }, descriptionKey = "MUTE_VOICE_6")
	private Boolean muteVoice6 = DEFAULT_MUTE_STEREO_VOICE2;

	@Parameter(names = { "--muteStereoVoice3", "-7" }, descriptionKey = "MUTE_VOICE_7")
	private Boolean muteVoice7 = DEFAULT_MUTE_STEREO_VOICE3;

	@Parameter(names = { "--muteStereoVoice4", "-8" }, descriptionKey = "MUTE_VOICE_8")
	private Boolean muteVoice8 = DEFAULT_MUTE_STEREO_VOICE4;

	@Parameter(names = { "--muteThirdSidVoice1", "-9" }, descriptionKey = "MUTE_VOICE_9")
	private Boolean muteVoice9 = DEFAULT_MUTE_THIRDSID_VOICE1;

	@Parameter(names = { "--muteThirdSidVoice2", "-10" }, descriptionKey = "MUTE_VOICE_10")
	private Boolean muteVoice10 = DEFAULT_MUTE_THIRDSID_VOICE2;

	@Parameter(names = { "--muteThirdSidVoice3", "-11" }, descriptionKey = "MUTE_VOICE_11")
	private Boolean muteVoice11 = DEFAULT_MUTE_THIRDSID_VOICE3;

	@Parameter(names = { "--muteThirdSidVoice4", "-12" }, descriptionKey = "MUTE_VOICE_12")
	private Boolean muteVoice12 = DEFAULT_MUTE_THIRDSID_VOICE4;

	@Parameter(names = { "--vbr" }, descriptionKey = "VBR", arity=1)
	protected Boolean vbr;

	@Parameter(names = { "--vbrQuality" }, descriptionKey = "VBR_QUALITY")
	protected Integer vbrQuality;

	@Parameter(names = { "--cbr" }, descriptionKey = "CBR")
	protected Integer cbr;

	@Parameter(names = { "--verbose", "-v" }, descriptionKey = "VERBOSE", validateWith = VerboseValidator.class)
	private Integer verbose = 0;

	@Parameter(names = { "--quiet", "-q" }, descriptionKey = "QUIET")
	private Boolean quiet = Boolean.FALSE;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	private ConsolePlayer(final String[] args) {
		try {
			JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
			commander.parse(args);
			Optional<String> filename = filenames.stream().findFirst();
			if (help || !filename.isPresent()) {
				commander.usage();
				printSoundcardDevices();
				exit(1);
			}
			if (loop && isRecording()) {
				System.out.println("Warning: Loop has been disabled while recording audio files!");
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
			config.getEmulationSection().setMuteVoice1(muteVoice1);
			config.getEmulationSection().setMuteVoice2(muteVoice2);
			config.getEmulationSection().setMuteVoice3(muteVoice3);
			config.getEmulationSection().setMuteVoice4(muteVoice4);
			config.getEmulationSection().setMuteStereoVoice1(muteVoice5);
			config.getEmulationSection().setMuteStereoVoice2(muteVoice6);
			config.getEmulationSection().setMuteStereoVoice3(muteVoice7);
			config.getEmulationSection().setMuteStereoVoice4(muteVoice8);
			config.getEmulationSection().setMuteThirdSIDVoice1(muteVoice9);
			config.getEmulationSection().setMuteThirdSIDVoice2(muteVoice10);
			config.getEmulationSection().setMuteThirdSIDVoice3(muteVoice11);
			config.getEmulationSection().setMuteThirdSIDVoice4(muteVoice12);
			if (vbr != null) {
				setMP3DriverSetting(mp3Driver->mp3Driver.setVbr(vbr));
			}
			if (vbrQuality != null) {
				setMP3DriverSetting(mp3Driver->mp3Driver.setVbrQuality(vbrQuality));
			}
			if (cbr != null) {
				setMP3DriverSetting(mp3Driver->mp3Driver.setCbr(cbr));
			}

			final SidTune tune = SidTune.load(new File(filename.get()));
			tune.getInfo().setSelectedSong(song);
			final Player player = new Player(config, cpuDebug ? MOS6510Debug.class : MOS6510.class);
			player.setTune(tune);
			player.getTimer().setStart(startTime);
			final ConsoleIO consoleIO = new ConsoleIO(config, filename.get());
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
			if (isRecording() && defaultLength <= 0
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

	public final void setMP3DriverSetting(final Consumer<MP3Driver> function) {
		function.accept((MP3Driver) Audio.MP3.getAudioDriver());
		function.accept((MP3Driver) ((ProxyDriver)Audio.LIVE_MP3.getAudioDriver()).getDriverTwo());
	}

	private void setSIDDatabase(final Player player) {
		String hvscRoot = player.getConfig().getSidplay2Section().getHvsc();
		if (hvscRoot != null) {
			File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
			try (FileInputStream input = new FileInputStream(file)) {
				player.setSidDatabase(new SidDatabase(input));
			} catch (IOException e) {
				System.err.println("WARNING: song length database can not be read: " + e.getMessage());
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
