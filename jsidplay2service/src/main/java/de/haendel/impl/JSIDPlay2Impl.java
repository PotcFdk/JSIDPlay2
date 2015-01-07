package de.haendel.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.ToIntFunction;

import javax.persistence.metamodel.SingularAttribute;

import libsidplay.Player;
import libsidplay.player.DriverSettings;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.STIL;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;
import libsidutils.SidDatabase;
import sidplay.audio.MP3Stream;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.collection.StilEntry;
import ui.entities.collection.StilEntry_;
import ui.entities.config.Configuration;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties SID_AUTHORS = new Properties();
	static {
		try (InputStream is = SidTune.class
				.getResourceAsStream("pictures.properties")) {
			SID_AUTHORS.load(is);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	private static final String ROOT_DIR = "/home/ken/Downloads/C64Music";
	private static final String HVSC_ROOT = ROOT_DIR + "/C64Music";

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
		player.setSidDatabase(getSidDatabase(player.getConfig().getSidplay2()
				.getHvsc()));
		player.setDriverSettings(new DriverSettings(new MP3Stream(out), config
				.getEmulation().getEmulation()));
		player.play(SidTune.load(getFile(resource)));
		player.waitForC64();
	}

	private SidDatabase getSidDatabase(String hvscRoot) throws IOException {
		if (hvscRoot != null) {
			File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
			try (FileInputStream input = new FileInputStream(file)) {
				return new SidDatabase(input);
			}
		}
		return null;
	}

	@Override
	public byte[] loadPhoto(String resource) throws IOException, SidTuneError {
		SidTune tune = SidTune.load(getFile(resource));
		if (tune != null) {
			Collection<String> photos = tune.getInfo().getInfoString();
			if (photos.size() > 1) {
				Iterator<String> iterator = photos.iterator();
				iterator.next();
				String author = iterator.next();
				String photo = SID_AUTHORS.getProperty(author);
				if (photo != null) {
					photo = "Photos/" + photo;
					ByteArrayOutputStream s = new ByteArrayOutputStream();
					try (InputStream is = SidTune.class
							.getResourceAsStream(photo)) {
						int n = 1;
						while (n > 0) {
							byte[] b = new byte[4096];
							n = is.read(b);
							if (n > 0)
								s.write(b, 0, n);
						}
					}
					return s.toByteArray();
				}
			}
		}
		return new byte[0];
	}

	@Override
	public Map<String, String> getTuneInfos(String resource)
			throws IOException, SidTuneError {
		Map<String, String> tuneInfos = new HashMap<String, String>();
		File tuneFile = getFile(resource);
		SidTune tune = SidTune.load(tuneFile);
		SidDatabase db = getSidDatabase(HVSC_ROOT);
		STIL stil = getSTIL(HVSC_ROOT);
		if (tune != null) {
			ToIntFunction<SidTune> lengthFnct = new ToIntFunction<SidTune>() {
				@Override
				public int applyAsInt(SidTune tn) {
					return db != null ? db.getFullSongLength(tn) : 0;
				}
			};
			HVSCEntry hvscEntry = new HVSCEntry(lengthFnct, "", tuneFile, tune);

			STILEntry stilEntry = db.getPath(tune) != null ? stil
					.getSTILEntry(db.getPath(tune)) : null;
			if (stilEntry != null) {
				// get STIL Global Comment
				hvscEntry.setStilGlbComment(stilEntry.globalComment);
				// add tune infos
				addSTILInfo(stilEntry.infos, tuneInfos);
				// go through subsongs & add them as well
				for (final TuneEntry entry : stilEntry.subtunes) {
					addSTILInfo(entry.infos, tuneInfos);
				}
			}
			for (Field field : HVSCEntry_.class.getDeclaredFields()) {
				if (field.getName().equals(HVSCEntry_.id.getName())) {
					continue;
				}
				if (!(SingularAttribute.class.isAssignableFrom(field.getType()))) {
					continue;
				}
				String name = HVSCEntry.class.getSimpleName() + "."
						+ field.getName();
				Object value = null;
				try {
					SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field
							.get(hvscEntry);
					value = ((Method) singleAttribute.getJavaMember())
							.invoke(hvscEntry);
				} catch (IllegalArgumentException | IllegalAccessException
						| InvocationTargetException e) {
				}
				tuneInfos.put(name, String.valueOf(value != null ? value : ""));
			}
		}
		return tuneInfos;
	}

	private STIL getSTIL(String hvscRoot) {
		try (FileInputStream input = new FileInputStream(new File(HVSC_ROOT,
				STIL.STIL_FILE))) {
			return new STIL(input);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void addSTILInfo(ArrayList<Info> infos,
			Map<String, String> tuneInfos) {
		for (Info info : infos) {
			ui.entities.collection.StilEntry stil = new ui.entities.collection.StilEntry();
			stil.setStilName(info.name);
			stil.setStilAuthor(info.author);
			stil.setStilTitle(info.title);
			stil.setStilArtist(info.artist);
			stil.setStilComment(info.comment);
			String value;
			value = info.name;
			String prefix = StilEntry.class.getSimpleName() + ".";
			tuneInfos.put(prefix+StilEntry_.stilName.getName(),
					String.valueOf(value != null ? value : ""));
			value = info.author;
			tuneInfos.put(prefix+StilEntry_.stilAuthor.getName(),
					String.valueOf(value != null ? value : ""));
			value = info.title;
			tuneInfos.put(prefix+StilEntry_.stilTitle.getName(),
					String.valueOf(value != null ? value : ""));
			value = info.artist;
			tuneInfos.put(prefix+StilEntry_.stilArtist.getName(),
					String.valueOf(value != null ? value : ""));
			value = info.comment;
			tuneInfos.put(prefix+StilEntry_.stilComment.getName(),
					String.valueOf(value != null ? value : ""));
		}
	}
}
