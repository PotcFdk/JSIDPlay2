package libsidutils.fingerprinting;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.common.SamplingRate;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.DebugUtil;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.WhatsSidDriver;
import sidplay.ini.IniConfig;
import ui.filefilter.TuneFileFilter;

/**
 * Beta: Shazam like feature: Analyze tunes to recognize a currently played tune
 * 
 * This is the test program.
 * 
 * @author ken
 *
 */
@Parameters(resourceBundle = "libsidutils.fingerprinting.FingerPrintingCreator")
public class FingerPrintingCreator {

	static {
		DebugUtil.init();
	}

	private static final TuneFileFilter TUNE_FILE_FILTER = new TuneFileFilter();

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--deleteAll" }, descriptionKey = "DELETE_ALL", arity = 1)
	private Boolean deleteAll = Boolean.FALSE;

	@ParametersDelegate
	private IniConfig config = new IniConfig(true, null);

	private Player player;

	private WhatsSidDriver whatsSidDriver;

	private void execute(String[] args) throws IOException, SidTuneError, InterruptedException {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}
		config.getAudioSection().setAudio(Audio.WHATS_SID);
		config.getAudioSection().setSamplingRate(SamplingRate.VERY_LOW);
		config.getSidplay2Section().setDefaultPlayLength(180);
		config.getSidplay2Section().setEnableDatabase(true);
		String hvsc = config.getSidplay2Section().getHvsc();

		player = new Player(config);
		player.setSidDatabase(hvsc != null ? new SidDatabase(hvsc) : null);

		whatsSidDriver = (WhatsSidDriver) Audio.WHATS_SID.getAudioDriver();

		if (Boolean.TRUE.equals(deleteAll)) {
			System.out.println("Delete all fingerprintings...");
			whatsSidDriver.deleteAll();
		}

		System.out.println("Create fingerprintings... (press q <return> to abort)");

		try {
			processDirectory(new File(hvsc));
		} catch (IOException e) {
			e.printStackTrace();
		}
		whatsSidDriver.dispose();

		System.exit(0);
	}

	private void processDirectory(File dir) throws IOException, SidTuneError {
		File[] listFiles = dir.listFiles();
		Arrays.sort(listFiles);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				processDirectory(file);
			} else if (file.isFile()) {
				if (TUNE_FILE_FILTER.accept(file)) {
					whatsSidDriver.setTuneFile(file);
					player.setRecordingFilenameProvider(tune -> {
						String filename = PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath());
						if (tune.getInfo().getSongs() > 1) {
							filename += String.format("-%02d", tune.getInfo().getCurrentSong());
						}
						return filename;
					});
					player.setTune(SidTune.load(file));
					player.getTune().getInfo().setSelectedSong(player.getTune().getInfo().getStartSong());
					player.startC64();
					player.stopC64(false);
				}
				if (System.in.available() > 0) {
					final int key = System.in.read();
					if (key == 'q') {
						throw new IOException("Termination after pressing q");
					}
				}
			}
		}
	}

	public static void main(String[] args) throws IOException, SidTuneError, InterruptedException {
		new FingerPrintingCreator().execute(args);
	}

}
