package libsidplay.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import libsidplay.components.c1541.DiskImage;
import libsidplay.components.cart.Cartridge;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.T64;
import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ini.intf.IConfig;
import applet.filefilter.DiskFileFilter;
import applet.filefilter.TuneFileFilter;

public class Directory {

	/**
	 * Should be true (C-1541) else false (C-1571).
	 */
	private boolean singleSided;
	/**
	 * Disk/Tape title.
	 */
	private byte[] title;
	/**
	 * Disk ID or null (tape).
	 */
	private byte[] id;
	/**
	 * Directory entries.
	 */
	private List<DirEntry> dirEntries;
	/**
	 * Free disk blocks or -1 (tape).
	 */
	private int freeBlocks;

	/**
	 * Status line to be displayed as an alternative to the freeBlocks.
	 */
	private String statusLine;

	public Directory() {
		dirEntries = new ArrayList<DirEntry>();
		singleSided = true;
		freeBlocks = -1;
	}

	public void setTitle(byte[] diskName) {
		title = diskName;
	}

	public void setId(byte[] diskID) {
		id = diskID;
	}

	public void setSingleSided(boolean single) {
		singleSided = single;
	}

	public void setFreeBlocks(int blocks) {
		freeBlocks = blocks;
	}

	public List<DirEntry> getDirEntries() {
		return dirEntries;
	}

	public String getStatusLine() {
		if (statusLine == null) {
			if (freeBlocks != -1) {
				return String.format("%-3d BLOCKS FREE.", freeBlocks);
			} else {
				return null;
			}
		} else {
			return statusLine;
		}
	}

	public void setStatusLine(final String line) {
		this.statusLine = line;
	}

	@Override
	public String toString() {
		StringBuilder header = new StringBuilder();
		header.append(singleSided ? "0 " : "1 ");
		header.append(DirEntry.convertFilename(title, -1));
		if (id != null) {
			header.append(" ");
			for (int i = 0; i < id.length; i++) {
				header.append((char) ((id[i] & 0xff)));
			}
		}
		return header.toString();
	}

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
	public static final Directory getDirectory(final File file,
			final IConfig cfg) throws IOException {
		if (file.getName().toLowerCase().endsWith(".gz")) {
			return gzToDir(file, cfg);
		} else if (file.getName().toLowerCase().endsWith(".zip")) {
			return zipToDir(file);
		} else if (diskFilter.accept(file)) {
			return DiskImage.getDirectory(file);
		} else if (file.getName().toLowerCase().endsWith(".t64")) {
			return T64.getDirectory(file);
		} else if (file.getName().toLowerCase().endsWith(".crt")) {
			return Cartridge.getDirectory(file);
		} else if (tuneFilter.accept(file)) {
			return SidTune.getDirectory(file, cfg);
		}
		return null;
	}

	protected static Directory gzToDir(final File file, final IConfig cfg)
			throws IOException {
		final File outFile = ZipEntryFileProxy.extractFromGZ(file);
		return getDirectory(outFile, cfg);
	}

	protected static Directory zipToDir(final File file) throws ZipException,
			IOException {
		Directory dir = new Directory();
		dir.setId(null);
		byte[] diskName = DirEntry.asciiTopetscii(
				getShortenedString(file.getName(), 20), Integer.MAX_VALUE);
		dir.setTitle(diskName);
		ZipFile zf = new ZipFile(file);
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
		zf.close();
		// Display a hint in the last line
		String hint = "PLEASE ENTER ZIP LIKE A DIR.";
		dir.setStatusLine(hint);
		return dir;
	}

	protected static String getShortenedString(String str, int max) {
		int extIdx = str.lastIndexOf('.');
		if (str.length() > max && extIdx != -1) {
			str = str.substring(0,
					Math.min(max - str.substring(extIdx).length(), extIdx))
					+ str.substring(extIdx);
		}
		return str;
	}
}
