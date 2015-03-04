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
import java.nio.file.Files;
import java.nio.file.Paths;
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
import sidplay.ini.IniConfig;
import sidplay.ini.intf.IFilterSection;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.collection.StilEntry;
import ui.entities.config.Configuration;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	private static final String ROOT_DIR = "/home/ken/Downloads/C64Music";
	private static final String HVSC_ROOT = ROOT_DIR + "/C64Music";

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
		File file = getAbsoluteFile(path);
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
			addPath(result, new File(file, "../"));
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
					result.add(path + (f.isDirectory() ? "/" : ""));
				} else {
					result.add("/");
				}
			}
		} catch (IOException e) {
			// ignore invalid path
		}
	}

	private File getAbsoluteFile(String path) {
		return new File(ROOT_DIR, path);
	}

	@Override
	public byte[] getFile(String path) throws IOException {
		return Files.readAllBytes(Paths.get(getAbsoluteFile(path).getPath()));
	}

	@Override
	public void convert(Configuration config, String resource,
			OutputStream out, int cbr, int vbr, boolean isVbr)
			throws InterruptedException, IOException, SidTuneError {
		Player player = new Player(config);
		player.setSidDatabase(getSidDatabase(HVSC_ROOT));
		MP3Stream mp3Stream = new MP3Stream(out, cbr, vbr, isVbr);
		player.setDriverSettings(new DriverSettings(mp3Stream));
		player.play(SidTune.load(getAbsoluteFile(resource)));
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
	public byte[] getPhoto(String resource) throws IOException, SidTuneError {
		SidTune tune = SidTune.load(getAbsoluteFile(resource));
		if (tune != null) {
			Collection<String> infos = tune.getInfo().getInfoString();
			if (infos.size() > 1) {
				Iterator<String> iterator = infos.iterator();
				/* title = */iterator.next();
				String author = iterator.next();
				String photo = SID_AUTHORS.getProperty(author);
				if (photo != null) {
					ByteArrayOutputStream s = new ByteArrayOutputStream();
					try (InputStream is = SidTune.class
							.getResourceAsStream("Photos/" + photo)) {
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
		File tuneFile = getAbsoluteFile(resource);
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
				hvscEntry.setStilGlbComment(stilEntry.globalComment);
				StringBuffer stilText = formatStilText(stilEntry);
				tuneInfos.put(StilEntry.class.getSimpleName() + ".text",
						stilText.toString());
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
			// ignore
		}
		return null;
	}

	private StringBuffer formatStilText(STILEntry stilEntry) {
		StringBuffer result = new StringBuffer();
		if (stilEntry != null) {
			// append STIL infos,replace multiple whitespaces
			String writeSTILEntry = writeSTILEntry(stilEntry);
			String replaceAll = writeSTILEntry.replaceAll("([ \t\r])+", " ");
			result.append(replaceAll);
		}
		return result;
	}

	private String writeSTILEntry(STILEntry stilEntry) {
		StringBuffer result = new StringBuffer();
		if (stilEntry.filename != null) {
			result.append("Filename: ");
			result.append(stilEntry.filename);
			result.append(" - ");
		}
		if (stilEntry.globalComment != null) {
			result.append("\n" + stilEntry.globalComment);
		}
		for (Info info : stilEntry.infos) {
			writeSTILEntry(result, info);
		}
		int subTuneNo = 1;
		for (TuneEntry entry : stilEntry.subtunes) {
			if (entry.globalComment != null) {
				result.append("\n" + entry.globalComment);
			}
			for (Info info : entry.infos) {
				result.append("\nSubTune #" + subTuneNo + ": ");
				writeSTILEntry(result, info);
			}
			subTuneNo++;
		}
		return result.append("                                        ")
				.toString();
	}

	private void writeSTILEntry(StringBuffer buffer, Info info) {
		if (info.name != null) {
			buffer.append("\nName: ");
			buffer.append(info.name);
		}
		if (info.author != null) {
			buffer.append("\nAuthor: ");
			buffer.append(info.author);
		}
		if (info.title != null) {
			buffer.append("\nTitle: ");
			buffer.append(info.title);
		}
		if (info.artist != null) {
			buffer.append("\nArtist: ");
			buffer.append(info.artist);
		}
		if (info.comment != null) {
			buffer.append("\nComment: ");
			buffer.append(info.comment.replaceAll("\"", "'"));
		}
	}

	@Override
	public List<String> getFilters() {
		List<String> result = new ArrayList<String>();
		IniConfig cfg = new IniConfig();
		List<? extends IFilterSection> filterSection = cfg.getFilter();
		for (Iterator<? extends IFilterSection> iterator = filterSection
				.iterator(); iterator.hasNext();) {
			final IFilterSection iFilterSection = (IFilterSection) iterator
					.next();
			if (iFilterSection.isReSIDFilter6581()) {
				result.add("RESID_MOS6581_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDFilter8580()) {
				result.add("RESID_MOS8580_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDfpFilter6581()) {
				result.add("RESIDFP_MOS6581_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDfpFilter8580()) {
				result.add("RESIDFP_MOS8580_" + iFilterSection.getName());
			}
		}
		return result;
	}

}
