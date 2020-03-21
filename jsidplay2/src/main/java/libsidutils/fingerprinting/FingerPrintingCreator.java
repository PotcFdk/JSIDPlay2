package libsidutils.fingerprinting;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
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
@Parameters(resourceBundle = "libsidutils.whatssid.WhatsSidAnalyser")
public class FingerPrintingCreator implements Function<SidTune, String> {

	private static final TuneFileFilter TUNE_FILE_FILTER = new TuneFileFilter();

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(descriptionKey = "DIRECTORY", required = true)
	private String directoryPath;

	@ParametersDelegate
	private IniConfig config = new IniConfig(true, null);

	private Player player;

	private String currentFilterName;

	private void execute(String[] args) throws IOException, SidTuneError, InterruptedException {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		player = new Player(config);
		player.setRecordingFilenameProvider(this);
		setSIDDatabase();

		System.out.println();
		System.out.println("Analyse...");

		for (File file : new File(directoryPath).listFiles()) {
			if (TUNE_FILE_FILTER.accept(file)) {

				SidTune sidTune = SidTune.load(file);
				player.setTune(sidTune);
				sidTune.getInfo().setSelectedSong(sidTune.getInfo().getStartSong());

				player.startC64();
				player.stopC64(false);
			}
		}
	}

	private void setSIDDatabase() {
		String hvscRoot = config.getSidplay2Section().getHvsc();
		if (hvscRoot != null) {
			try {
				player.setSidDatabase(new SidDatabase(hvscRoot));
			} catch (IOException e) {
				System.err.println("WARNING: song length database can not be read: " + e.getMessage());
			}
		}
	}

	@Override
	public String apply(SidTune tune) {
		String defaultName = "jsidplay2";
		if (tune == SidTune.RESET) {
			return new File(directoryPath, defaultName).getAbsolutePath();
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> infos = info.getInfoString().iterator();
		String name = infos.hasNext() ? infos.next().replaceAll("[:\\\\/*?|<>]", "_") : defaultName;
		String filename = new File(directoryPath, PathUtils.getFilenameWithoutSuffix(name)).getAbsolutePath();
		filename += currentFilterName != null ? "_" + currentFilterName : "";
		if (info.getSongs() > 1) {
			filename += String.format("-%02d", info.getCurrentSong());
		}
		return filename;
	}

	public static void main(String[] args) throws IOException, SidTuneError, InterruptedException {
		new FingerPrintingCreator().execute(args);
	}

}