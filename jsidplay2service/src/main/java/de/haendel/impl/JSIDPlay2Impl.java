package de.haendel.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import libsidplay.Player;
import libsidplay.player.DriverSettings;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.SidDatabase;
import sidplay.audio.MP3Stream;
import ui.entities.config.Configuration;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	private static final String ROOT_DIR = "/home/ken/Downloads/C64Music";

	private static class FileTypeComparator implements Comparator<File> {

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

	private static final Comparator<File> DIRECTORY_COMPARATOR = new FileTypeComparator();

	@Override
	public List<String> getDirectory(String path, String filter) {
		File file = getFile(path);
		File[] listFiles = file.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() || filter == null
						|| pathname.getName().matches(filter);
			}
		});
		ArrayList<String> result = new ArrayList<String>();
		if (listFiles != null) {
			List<File> asList = Arrays.asList(listFiles);
			Collections.sort(asList, DIRECTORY_COMPARATOR);
			addPath(result, new File(file, ".."));
			for (File f : asList) {
				addPath(result, f);
			}
		}
		return result;
	}

	private void addPath(ArrayList<String> result, File f) {
		try {
			String canonicalPath = f.getCanonicalPath();
			if (canonicalPath.startsWith(ROOT_DIR)) {
				String path = canonicalPath.substring(ROOT_DIR.length());
				if (!path.isEmpty()) {
					result.add(path);
				} else {
					result.add("/");
				}
			}
		} catch (IOException e) {
			// ignore invalid path
		}
	}

	@Override
	public File getFile(String path) {
		return new File(ROOT_DIR, path);
	}

	@Override
	public void convert(Configuration config, String resource, OutputStream out)
			throws InterruptedException, IOException, SidTuneError {
		Player player = new Player(config);
		setSIDDatabase(player);
		player.setDriverSettings(new DriverSettings(new MP3Stream(out), config
				.getEmulation().getEmulation()));
		player.play(SidTune.load(getFile(resource)));
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
