package libsidutils.directory;

import static libsidutils.directory.DirEntry.FILETYPE_NONE;
import static libsidutils.directory.DirEntry.convertFilename;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * This class represents a Floppy 1541/1571 disk directory.
 *
 * @author Ken Händel
 *
 */
public abstract class Directory {

	/**
	 * Should be true (C-1541) else false (C-1571).
	 */
	protected boolean singleSided = true;
	/**
	 * Disk/Tape title.
	 */
	protected byte[] title;
	/**
	 * Disk ID or null (tape).
	 */
	protected byte[] id;
	/**
	 * Directory entries.
	 */
	protected Collection<DirEntry> dirEntries = new ArrayList<>();

	/**
	 * Status line to displayed free blocks.
	 */
	protected String statusLine;

	public String getTitle() {
		StringBuilder header = new StringBuilder();
		header.append(singleSided ? "0 " : "1 ");
		header.append(convertFilename(title, FILETYPE_NONE));
		if (id != null) {
			header.append(" ");
			for (byte element : id) {
				header.append((char) (element & 0xff));
			}
		}
		return header.toString();
	}

	public Collection<DirEntry> getDirEntries() {
		return dirEntries;
	}

	public String getStatusLine() {
		return statusLine;
	}

}
