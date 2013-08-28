package ui.directory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import libsidplay.components.DirEntry;
import libsidplay.components.Directory;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.cart.Cartridge;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.T64;
import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ini.intf.IConfig;
import ui.entities.collection.HVSCEntry;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.TuneFileFilter;

public class PseudoDirectory {

	/**
	 * Char-set for string to byte conversions.
	 */
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");
	/**
	 * Detect SID tunes for preview.
	 */
	private static TuneFileFilter tuneFilter = new TuneFileFilter();
	private static DiskFileFilter diskFilter = new DiskFileFilter();

	/**
	 * Create a directory preview of the currently selected file
	 * 
	 * @param file
	 *            file to create a directory preview for
	 * @return directory preview
	 * @throws IOException
	 *             can not open file
	 */
	public static final Directory getDirectory(File hvscRoot, final File file,
			final IConfig cfg) throws IOException {
		if (file.getName().toLowerCase().endsWith(".gz")) {
			return gzToDir(hvscRoot, file, cfg);
		} else if (file.getName().toLowerCase().endsWith(".zip")) {
			return zipToDir(file);
		} else if (diskFilter.accept(file)) {
			return DiskImage.getDirectory(file);
		} else if (file.getName().toLowerCase().endsWith(".t64")) {
			return T64.getDirectory(file);
		} else if (file.getName().toLowerCase().endsWith(".crt")) {
			return Cartridge.getDirectory(file);
		} else if (tuneFilter.accept(file)) {
			return getTuneAsDirectory(hvscRoot, file, cfg);
		}
		return null;
	}

	private static Directory gzToDir(File hvscRoot, final File file,
			final IConfig cfg) throws IOException {
		final File outFile = ZipEntryFileProxy.extractFromGZ(file, cfg
				.getSidplay2().getTmpDir());
		return getDirectory(hvscRoot, outFile, cfg);
	}

	private static Directory zipToDir(final File file) throws ZipException,
			IOException {
		Directory dir = new Directory();
		dir.setId(null);
		byte[] diskName = DirEntry.asciiTopetscii(
				getShortenedString(file.getName(), 20), Integer.MAX_VALUE);
		dir.setTitle(diskName);
		try (ZipFile zf = new ZipFile(file)) {
			@SuppressWarnings("rawtypes")
			Enumeration en = zf.entries();
			while (en.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) en.nextElement();
				String dirEntryStr = ze.getName();
				dirEntryStr = getShortenedString(dirEntryStr, 20);
				DirEntry dirEntry = new DirEntry(0, DirEntry.asciiTopetscii(
						dirEntryStr, Integer.MAX_VALUE), (byte) -1) {
					@Override
					public void save(File autostartFile) throws IOException {
					}
				};
				// Display a pseudo directory entry for each ZIP entry
				dir.getDirEntries().add(dirEntry);
			}
		}
		// Display a hint in the last line
		String hint = "PLEASE ENTER ZIP LIKE A DIR.";
		dir.setStatusLine(hint);
		return dir;
	}

	private static Directory getTuneAsDirectory(File hvscRoot, File file,
			IConfig cfg) throws IOException {
		Directory dir = new Directory();
		SidTune tune;
		try {
			tune = SidTune.load(file);
			if (tune == null) {
				throw new IOException();
			}
		} catch (SidTuneError e) {
			throw new IOException();
		}
		HVSCEntry entry = HVSCEntry.create(hvscRoot, file.getAbsolutePath(),
				file, tune);
		final String title = entry.getTitle() != null ? entry.getTitle()
				: entry.getName();

		// Directory title: tune title or filename
		dir.setTitle(DirEntry.asciiTopetscii(title, 16));
		// Directory id: start song '/' song count
		dir.setId((String.valueOf(entry.getStartSong()) + "/" + String
				.valueOf(Math.max(1, entry.getNoOfSongs()))).getBytes(ISO88591));
		List<DirEntry> entries = dir.getDirEntries();

		addProperty(entries, "TITLE", entry.getTitle());
		addProperty(entries, "AUTHOR", entry.getAuthor());
		addProperty(entries, "RELEASED", entry.getReleased());
		addProperty(entries, "FORMAT", entry.getFormat());
		addProperty(entries, "PLAYERID", entry.getPlayerId());
		addProperty(entries, "NO_SONGS", String.valueOf(entry.getNoOfSongs()));
		addProperty(entries, "STARTSONG", String.valueOf(entry.getStartSong()));
		addProperty(entries, "CLOCKFREQ", String.valueOf(entry.getClockFreq()));
		addProperty(entries, "SPEED", String.valueOf(entry.getSpeed()));
		addProperty(entries, "SIDMODEL1", String.valueOf(entry.getSidModel1()));
		addProperty(entries, "SIDMODEL2", String.valueOf(entry.getSidModel2()));
		addProperty(entries, "COMPAT", String.valueOf(entry.getCompatibility()));
		addProperty(entries, "TUNE_LGTH", String.valueOf(entry.getTuneLength()));
		addProperty(entries, "AUDIO", entry.getAudio());
		addProperty(entries, "CHIP_BASE1",
				String.valueOf(entry.getSidChipBase1()));
		addProperty(entries, "CHIP_BASE2",
				String.valueOf(entry.getSidChipBase2()));
		addProperty(entries, "DRV_ADDR",
				String.valueOf(entry.getDriverAddress()));
		addProperty(entries, "LOAD_ADDR",
				String.valueOf(entry.getLoadAddress()));
		addProperty(entries, "LOAD_LGTH", String.valueOf(entry.getLoadLength()));
		addProperty(entries, "INIT_ADDR",
				String.valueOf(entry.getInitAddress()));
		addProperty(entries, "PLY_ADDR",
				String.valueOf(entry.getPlayerAddress()));
		addProperty(entries, "FILE_DATE", String.valueOf(entry.getFileDate()));
		addProperty(entries, "SIZE_KB", String.valueOf(entry.getFileSizeKb()));
		addProperty(entries, "SIZE_B", String.valueOf(entry.getTuneSizeB()));
		addProperty(entries, "RELOC_PAGE",
				String.valueOf(entry.getRelocStartPage()));
		addProperty(entries, "RELOC_PAGES",
				String.valueOf(entry.getRelocNoPages()));

		return dir;
	}

	private static void addProperty(List<DirEntry> entries, String property,
			String value) {
		byte[] filename = DirEntry.asciiTopetscii(property + "=" + value, 20);
		// Pseudo directory entry: tune property '=' value
		entries.add(new DirEntry(0, filename, (byte) -1) {
			@Override
			public void save(File autostartFile) throws IOException {
			}
		});
	}

	private static String getShortenedString(String str, int max) {
		int extIdx = str.lastIndexOf('.');
		if (str.length() > max && extIdx != -1) {
			str = str.substring(0,
					Math.min(max - str.substring(extIdx).length(), extIdx))
					+ str.substring(extIdx);
		}
		return str;
	}

}
