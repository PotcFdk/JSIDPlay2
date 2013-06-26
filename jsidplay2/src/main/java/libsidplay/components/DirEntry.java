package libsidplay.components;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public abstract class DirEntry {
	/**
	 * Char-set for string to byte conversions.
	 */
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");

	/**
	 * BITMASK_FILETYPE.
	 */
	public static final byte BITMASK_FILETYPE = (byte) 0x7;
	/**
	 * FILETYPE_DEL.
	 */
	public static final byte FILETYPE_DEL = (byte) 0x00;

	/**
	 * FILETYPE_SEQ.
	 */
	public static final byte FILETYPE_SEQ = (byte) 0x01;

	/**
	 * FILETYPE_PRG.
	 */
	public static final byte FILETYPE_PRG = (byte) 0x02;

	/**
	 * FILETYPE_USR.
	 */
	public static final byte FILETYPE_USR = (byte) 0x03;

	/**
	 * FILETYPE_REL.
	 */
	public static final byte FILETYPE_REL = (byte) 0x04;

	/**
	 * All file extensions.
	 */
	private static final byte[][] FILETYPES = new byte[][] {
			{ 0x44, 0x45, 0x4c }, /* .DEL */
			{ 0x53, 0x45, 0x51 }, /* .SEQ */
			{ 0x50, 0x52, 0x47 }, /* .PRG */
			{ 0x55, 0x53, 0x52 }, /* .USR */
			{ 0x52, 0x45, 0x4c }, /* .REL */
	};

	/**
	 * Used disk blocks (disk) or number of bytes (tape).
	 */
	private int blocks;
	/**
	 * File name.
	 */
	private byte[] filename;
	/**
	 * File type.
	 */
	private byte fileType;

	/**
	 * Constructor.
	 * 
	 * @param nrSectors
	 *            disk: blocks used, tape: program length
	 * @param fn
	 *            file name
	 * @param fType
	 *            file type or -1 (no extension)
	 */
	public DirEntry(int nrSectors, byte[] fn, byte fType) {
		blocks = nrSectors;
		filename = fn;
		fileType = fType;
	}

	/**
	 * Quoted file name and type string.
	 * 
	 * @param fileName
	 *            file name
	 * @param fileType
	 *            file type
	 * @return quoted file name and type string
	 */
	public final static String convertFilename(final byte[] fileName,
			final int fileType) {
		StringBuffer fn = new StringBuffer();
		// BEGIN include filename in quotes
		fn.append((char) (0x22));
		for (int i = 0; i < fileName.length; i++) {
			if (fileName[i] == '\r' || fileName[i] == 0x00) {
				// newline or zero bytes are included
				// in tape descriptions and delimits the filename for us
				break;
			}
			// Beware the PETSCII bytes here!
			fn.append((char) ((fileName[i] & 0xff)));
		}
		// END include filename in quotes
		fn.append((char) (0x22));
		if (fileType != -1) {
			// append extension if applicable
			int ft = fileType & BITMASK_FILETYPE;
			if (ft >= FILETYPE_DEL && ft <= FILETYPE_REL) {
				// " DEL" | "PRG" ...
				fn.append((char) (0x20));
				for (int i = 0; i < FILETYPES[ft - FILETYPE_DEL].length; i++) {
					fn.append((char) (FILETYPES[ft - FILETYPE_DEL][i] & 0xff));
				}
			} else {
				// " ?"
				fn.append((char) (0x20));
				fn.append((char) (0x3f));
			}
		}
		return fn.toString();
	}

	/**
	 * Convert ASCII string to PETSCII bytes.
	 * 
	 * @param str
	 *            string to convert
	 * @param maxLen
	 *            maximum string length to take into account
	 * @return PETSCII bytes
	 */
	public final static byte[] asciiTopetscii(final String str, int maxLen) {
		return str.substring(0, Math.min(maxLen, str.length())).toUpperCase()
				.replace('_', '-').getBytes(ISO88591);
	}

	/**
	 * Get string representation of this directory entry.
	 */
	public String toString() {
		return String.format("%-3d  %s", blocks,
				convertFilename(filename, fileType));
	}

	/**
	 * Return a valid filename to save this directory entry to hard disk.
	 * 
	 * @return a valid filename to save this directory entry
	 */
	public final String getValidFilename() {
		final String convertFilename = convertFilename(filename, -1);
		return convertFilename.substring(1, convertFilename.length() - 1)
				.replace('/', '_');
	}
	
	/**
	 * Save the program of this directory entry to the specified file.
	 * 
	 * @param autostartFile
	 *            file to save
	 * @throws IOException
	 *             File write error
	 */
	public abstract void save(File autostartFile) throws IOException;
}
