package de.haendel.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import libsidplay.Player;
import libsidplay.player.DriverSettings;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.SidDatabase;
import sidplay.audio.Audio;
import sidplay.audio.MP3Stream;
import ui.entities.config.Configuration;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	private class FileTypeComparator implements Comparator<File> {

		@Override
		public int compare(File file1, File file2) {

			if (file1.isDirectory() && file2.isFile())
				return -1;
			if (file1.isDirectory() && file2.isDirectory()) {
				return file1.getName().compareToIgnoreCase(file2.getName());
			}
			if (file1.isFile() && file2.isFile()) {
				return file1.getName().compareToIgnoreCase(file2.getName());
			}
			return 1;
		}
	}

	private Comparator<File> cmp = new FileTypeComparator();

	@Override
	public List<File> getDirectory(String root, String filter) {
		File rootFile = new File(root);
		List<File> asList = Arrays.asList(rootFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() || filter == null
						|| pathname.getName().matches(filter);
			}
		}));
		Collections.sort(asList, cmp);
		return asList;
	}

	@Override
	public void convert(Configuration config, String resource, OutputStream out)
			throws InterruptedException, IOException, SidTuneError {
		config.getAudio().setAudio(Audio.NONE);
		Player player = new Player(config);
		setSIDDatabase(player);

		player.setDriverSettings(new DriverSettings(new MP3Stream(out), config
				.getEmulation().getEmulation()));
		player.play(SidTune.load(new File(resource)));
		player.waitForC64();
	}

	private void setSIDDatabase(final Player player) throws IOException {
		String hvscRoot = player.getConfig().getSidplay2().getHvsc();
		if (hvscRoot != null) {
			File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
			try (FileInputStream input = new FileInputStream(file)) {
				player.setSidDatabase(new SidDatabase(input));
			}
		}
	}
}
