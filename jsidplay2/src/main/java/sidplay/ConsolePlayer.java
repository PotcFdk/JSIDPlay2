package sidplay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sound.sampled.Mixer.Info;

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
import sidplay.consoleplayer.ConsoleIO;
import sidplay.consoleplayer.VerboseValidator;
import sidplay.ini.IniConfig;

@Parameters(resourceBundle = "sidplay.ConsolePlayer")
final public class ConsolePlayer {
	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = "--cpuDebug", hidden = true, descriptionKey = "DEBUG")
	private Boolean cpuDebug = Boolean.FALSE;

	@Parameter(names = { "--recordingFilename", "-r" }, descriptionKey = "RECORDING_FILENAME")
	private String recordingFilename = "jsidplay2";

	@Parameter(names = { "--startSong", "-o" }, descriptionKey = "START_SONG")
	private Integer song = null;

	@Parameter(names = { "--verbose", "-v" }, descriptionKey = "VERBOSE", validateWith = VerboseValidator.class)
	private Integer verbose = 0;

	@Parameter(names = { "--quiet", "-q" }, descriptionKey = "QUIET")
	private Boolean quiet = Boolean.FALSE;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	private ConsolePlayer(final String[] args) {
		try {
			final IniConfig config = new IniConfig(true);
			JCommander commander = JCommander.newBuilder().addObject(this).addObject(config)
					.addObject(Audio.MP3.getAudioDriver()).programName(getClass().getName()).build();
			commander.parse(args);
			Optional<String> filename = filenames.stream().findFirst();
			if (help || !filename.isPresent()) {
				commander.usage();
				printSoundcardDevices();
				exit(1);
			}
			final SidTune tune = SidTune.load(new File(filename.get()));
			tune.getInfo().setSelectedSong(song);
			final Player player = new Player(config, cpuDebug ? MOS6510Debug.class : MOS6510.class);
			player.setTune(tune);
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
			player.startC64();
		} catch (ParameterException | IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			exit(1);
		}
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

	private void printSoundcardDevices() {
		int deviceIdx = 0;
		for (Info device : JavaSound.getDevices()) {
			System.out.printf("    --deviceIndex %d -> %s (%s)\n", (deviceIdx++), device.getName(),
					device.getDescription());
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
