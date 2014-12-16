package de.haendel.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import libsidplay.Player;
import libsidplay.player.DriverSettings;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.SidDatabase;
import sidplay.audio.Audio;
import sidplay.audio.MP3File;
import sidplay.audio.RecordingFilenameProvider;
import ui.entities.config.Configuration;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	private static final String TMP_PREFIX = "jsidplay2";
	private static final String TMP_SUFFIX = ".jboss.mp3";
	private static final String HVSC = "/home/ken/Downloads/C64Music";

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
	public List<File> getDirectory(String root) {
		File rootFile = new File(root);
		List<File> asList = Arrays.asList(rootFile.listFiles());
		Collections.sort(asList, cmp);
		return asList;
	}

	@Override
	public byte[] convert(Configuration config, String resource)
			throws InterruptedException, IOException, SidTuneError {
		File file = null;
		try {
			file = File.createTempFile(TMP_PREFIX, TMP_SUFFIX);
			file.deleteOnExit();

			config.getSidplay2().setLoop(false);
			config.getSidplay2().setSingle(true);
			config.getSidplay2().setDefaultPlayLength(300);
			config.getSidplay2().setEnableDatabase(true);
			config.getSidplay2().setHvsc(HVSC);
			config.getAudio().setAudio(Audio.NONE);
			Player player = new Player(config);
			setSIDDatabase(player);
			setRecordingFilenameProvider(player, file);

			player.setDriverSettings(new DriverSettings(new MP3File(), config
					.getEmulation().getEmulation()));
			player.play(SidTune.load(new File(resource)));
			player.waitForC64();
			return Files.readAllBytes(Paths.get(file.getPath()));
		} finally {
			if (file != null) {
				file.delete();
			}
		}
	}

	private void setRecordingFilenameProvider(Player player, File file) {
		player.setRecordingFilenameProvider(new RecordingFilenameProvider() {
			@Override
			public String getFilename(SidTune theTune) {
				String filename = new File(file.getParentFile(), PathUtils
						.getBaseNameNoExt(file.getName())).getAbsolutePath();
				return filename;
			}
		});
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
