package sidplay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

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

	@Parameter(names = { "--recordingFilename", "-r" }, descriptionKey = "RECORDING_FILENAME")
	private String recordingFilename = "jsidplay2";

	@Parameter(names = { "--startSong", "-o" }, descriptionKey = "START_SONG")
	private Integer song = null;

	@Parameter(names = { "--startTime", "-t" }, descriptionKey = "START_TIME", converter = ParameterTimeConverter.class)
	private Integer startTime = 0;

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
			final IniConfig config = new IniConfig(true);
			JCommander commander = JCommander.newBuilder().addObject(this).addObject(config.getSidplay2Section())
					.addObject(config.getAudioSection()).addObject(config.getEmulationSection())
					.programName(getClass().getName()).build();
			commander.parse(args);
			Optional<String> filename = filenames.stream().findFirst();
			if (help || !filename.isPresent()) {
				commander.usage();
				printSoundcardDevices();
				exit(1);
			}
			if (config.getSidplay2Section().isLoop() && isRecording(config.getAudioSection().getAudio())) {
				System.out.println("Warning: Loop has been disabled while recording audio files!");
				config.getSidplay2Section().setLoop(false);
			}
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
			if (isRecording(config.getAudioSection().getAudio())
					&& config.getSidplay2Section().getDefaultPlayLength() <= 0
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
			try {
				player.setSidDatabase(new SidDatabase(hvscRoot));
			} catch (IOException e) {
				System.err.println("WARNING: song length database can not be read: " + e.getMessage());
			}
		}
	}

	private boolean isRecording(Audio audio) {
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
