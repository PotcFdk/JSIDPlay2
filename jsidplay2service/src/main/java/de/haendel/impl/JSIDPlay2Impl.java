package de.haendel.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import sidplay.audio.MP3Stream;
import sidplay.audio.RecordingFilenameProvider;
import ui.entities.config.Configuration;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	private static final String TMP_PREFIX = "jsidplay2";
	private static final String TMP_SUFFIX = ".jboss.mp3";

	private static final int DEFALT_LENGTH = 300;

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
	public byte[] convert(Configuration config, String resource, String hvsc)
			throws InterruptedException, IOException, SidTuneError {
		File file = null;
		try {
			file = File.createTempFile(TMP_PREFIX, TMP_SUFFIX);
			file.deleteOnExit();

			config.getAudio().setAudio(Audio.NONE);
			config.getSidplay2().setLoop(false);
			config.getSidplay2().setSingle(true);
			if (config.getSidplay2().getDefaultPlayLength() == 0) {
				config.getSidplay2().setDefaultPlayLength(DEFALT_LENGTH);
			}
			config.getSidplay2().setEnableDatabase(hvsc != null);
			if (hvsc != null) {
				config.getSidplay2().setHvsc(hvsc);
			}
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

	@Override
	public void convert2(Configuration config, String resource, String hvsc,
			OutputStream out) throws InterruptedException, IOException,
			SidTuneError {
		config.getAudio().setAudio(Audio.NONE);
		config.getSidplay2().setDefaultPlayLength(0);
		Player player = new Player(config);

		player.setDriverSettings(new DriverSettings(new MP3Stream(out), config
				.getEmulation().getEmulation()));
		player.play(SidTune.load(new File(resource)));
		player.waitForC64();
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
