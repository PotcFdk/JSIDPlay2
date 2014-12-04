package libsidplay.components;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * This class represents a disk directory. However it is used to show tape file
 * entries or other formats as a pseudo directory as well.
 * 
 * @author Ken Händel
 *
 */
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
	private Collection<DirEntry> dirEntries;

	/**
	 * Status line to be displayed as an alternative to the freeBlocks.
	 */
	private String statusLine;

	public Directory() {
		dirEntries = new ArrayList<DirEntry>();
		singleSided = true;
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

	public Collection<DirEntry> getDirEntries() {
		return dirEntries;
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setFreeBlocks(int blocks) {
		statusLine = String.format("%-3d BLOCKS FREE.", blocks);
	}

	public void setStatusLine(final String line) {
		statusLine = line;
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
