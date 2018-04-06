package de.haendel.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.IntSupplier;

import libsidplay.config.IConfig;
import libsidplay.config.IFilterSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import libsidutils.stil.STIL.TuneEntry;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.ini.IniConfig;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.StilEntry;

public class JSIDPlay2Impl implements IJSIDPlay2 {

	private static final String ROOT_DIR = "/home/ken/Downloads/C64Music";
	private static final String HVSC_ROOT = ROOT_DIR + "/C64Music";

	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties SID_AUTHORS = new Properties();

	static {
		try (InputStream is = SidTune.class.getResourceAsStream("pictures.properties")) {
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
				if (pathname.isDirectory() && pathname.getName().endsWith(".tmp"))
					return false;
				return pathname.isDirectory() || filter == null || pathname.getName().matches(filter);
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
	public void convert(IConfig config, String resource, AudioDriver driver) throws IOException, SidTuneError {
		Player player = new Player(config);
		player.setSidDatabase(new SidDatabase(HVSC_ROOT));
		player.setAudioDriver(driver);
		player.play(SidTune.load(getAbsoluteFile(resource)));
		player.stopC64(false);
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
					try (InputStream is = SidTune.class.getResourceAsStream("Photos/" + photo)) {
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
	public Map<String, String> getTuneInfos(String resource) throws IOException, SidTuneError {
		Map<String, String> tuneInfos = new HashMap<String, String>();
		File tuneFile = getAbsoluteFile(resource);
		SidTune tune = SidTune.load(tuneFile);
		SidDatabase db = new SidDatabase(HVSC_ROOT);
		STIL stil = getSTIL(HVSC_ROOT);
		if (tune != null) {
			IntSupplier lengthFnct = new IntSupplier() {
				@Override
				public int getAsInt() {
					return db.getTuneLength(tune);
				}
			};
			HVSCEntry hvscEntry = new HVSCEntry(lengthFnct, "", tuneFile, tune);

			String path = db.getPath(tune);
			STILEntry stilEntry = path.length() > 0 ? stil.getSTILEntry(path) : null;
			if (stilEntry != null) {
				hvscEntry.setStilGlbComment(stilEntry.globalComment);
				StringBuffer stilText = formatStilText(stilEntry);
				tuneInfos.put(StilEntry.class.getSimpleName() + ".text", stilText.toString());
			}
			addTuneInfo(tuneInfos, "HVSCEntry.path", hvscEntry.getPath());
			addTuneInfo(tuneInfos, "HVSCEntry.name", hvscEntry.getName());
			addTuneInfo(tuneInfos, "HVSCEntry.title", hvscEntry.getTitle());
			addTuneInfo(tuneInfos, "HVSCEntry.author", hvscEntry.getAuthor());
			addTuneInfo(tuneInfos, "HVSCEntry.released", hvscEntry.getReleased());
			addTuneInfo(tuneInfos, "HVSCEntry.format", hvscEntry.getFormat());
			addTuneInfo(tuneInfos, "HVSCEntry.playerId", hvscEntry.getPlayerId());
			addTuneInfo(tuneInfos, "HVSCEntry.noOfSongs", hvscEntry.getNoOfSongs());
			addTuneInfo(tuneInfos, "HVSCEntry.startSong", hvscEntry.getStartSong());
			addTuneInfo(tuneInfos, "HVSCEntry.clockFreq", hvscEntry.getClockFreq());
			addTuneInfo(tuneInfos, "HVSCEntry.speed", hvscEntry.getSpeed());
			addTuneInfo(tuneInfos, "HVSCEntry.sidModel1", hvscEntry.getSidModel1());
			addTuneInfo(tuneInfos, "HVSCEntry.sidModel2", hvscEntry.getSidModel2());
			addTuneInfo(tuneInfos, "HVSCEntry.sidModel3", hvscEntry.getSidModel3());
			addTuneInfo(tuneInfos, "HVSCEntry.compatibility", hvscEntry.getCompatibility());
			addTuneInfo(tuneInfos, "HVSCEntry.tuneLength", hvscEntry.getTuneLength());
			addTuneInfo(tuneInfos, "HVSCEntry.audio", hvscEntry.getAudio());
			addTuneInfo(tuneInfos, "HVSCEntry.sidChipBase1", hvscEntry.getSidChipBase1());
			addTuneInfo(tuneInfos, "HVSCEntry.sidChipBase2", hvscEntry.getSidChipBase2());
			addTuneInfo(tuneInfos, "HVSCEntry.sidChipBase3", hvscEntry.getSidChipBase3());
			addTuneInfo(tuneInfos, "HVSCEntry.driverAddress", hvscEntry.getDriverAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.loadAddress", hvscEntry.getLoadAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.loadLength", hvscEntry.getLoadLength());
			addTuneInfo(tuneInfos, "HVSCEntry.initAddress", hvscEntry.getInitAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.playerAddress", hvscEntry.getPlayerAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.fileDate", hvscEntry.getFileDate());
			addTuneInfo(tuneInfos, "HVSCEntry.fileSizeKb", hvscEntry.getFileSizeKb());
			addTuneInfo(tuneInfos, "HVSCEntry.tuneSizeB", hvscEntry.getTuneSizeB());
			addTuneInfo(tuneInfos, "HVSCEntry.relocStartPage", hvscEntry.getRelocStartPage());
			addTuneInfo(tuneInfos, "HVSCEntry.relocNoPages", hvscEntry.getRelocNoPages());
			addTuneInfo(tuneInfos, "HVSCEntry.stilGlbComment", hvscEntry.getStilGlbComment());
		}
		return tuneInfos;
	}

	private void addTuneInfo(Map<String, String> tuneInfos, String name, Object value) {
		tuneInfos.put(name, String.valueOf(value != null ? value : ""));
	}

	private STIL getSTIL(String hvscRoot) {
		try (FileInputStream input = new FileInputStream(new File(HVSC_ROOT, STIL.STIL_FILE))) {
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
		return result.append("                                        ").toString();
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
		List<? extends IFilterSection> filterSection = cfg.getFilterSection();
		for (Iterator<? extends IFilterSection> iterator = filterSection.iterator(); iterator.hasNext();) {
			final IFilterSection iFilterSection = (IFilterSection) iterator.next();
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
