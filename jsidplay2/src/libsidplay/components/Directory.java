package libsidplay.components;

import java.util.ArrayList;
import java.util.List;

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

}
