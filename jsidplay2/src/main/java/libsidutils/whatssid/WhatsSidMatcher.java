package libsidutils.whatssid;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.ini.IniConfig;

public class WhatsSidMatcher {

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(descriptionKey = "FILENAME", required = true)
	private String filename;

	@ParametersDelegate
	private IniConfig config = new IniConfig(true, null);

	private Player player;

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
		setSIDDatabase();

		System.out.println();
		System.out.println("Match...");

		SidTune sidTune = SidTune.load(new File(filename));
		player.setTune(sidTune);
		sidTune.getInfo().setSelectedSong(sidTune.getInfo().getStartSong());

		runC64();
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

	private void runC64() throws IOException, SidTuneError, InterruptedException {
		player.startC64();
		player.stopC64(false);
	}

	public static void main(String[] args) throws IOException, SidTuneError, InterruptedException {
		new WhatsSidMatcher().execute(args);
	}
}
