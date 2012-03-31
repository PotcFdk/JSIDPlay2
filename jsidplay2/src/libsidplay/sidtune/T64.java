package libsidplay.sidtune;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.List;

import libsidplay.components.DirEntry;
import libsidplay.components.Directory;

public class T64 extends Prg {
	/**
	 * Char-set for string to byte conversions.
	 */
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");
	
	/**
	 * T64 program data.
	 */
	private byte[] data;
	private byte[] lastEntryName;
	
	protected static SidTune load(final String fileName, final byte[] dataBuf)
			throws SidTuneError {
		if (fileName != null) {
			final int lastIndexOf = fileName.lastIndexOf(".");
			final String ext = lastIndexOf != -1 ? fileName
					.substring(lastIndexOf) : "";
			if (!ext.equalsIgnoreCase(".t64")) {
				return null;
			}
		}

		final T64 sidtune = new T64();
		
		// always load first entry:
		if (!sidtune.getEntry(dataBuf, 1)) {
			return null;
		}

		// Automatic settings
		sidtune.info.songs = 1;
		sidtune.info.startSong = 1;
		sidtune.info.numberOfInfoStrings = 0;
		sidtune.program = dataBuf;

		// Create the speed/clock setting table.
		sidtune.convertOldStyleSpeedToTables(~0);
		return sidtune;
	}

	/**
	 * Load T64 file entry from buffer.
	 * 
	 * @param dataBuf
	 *            buffer with file data
	 * @param entryNum
	 *            entry number to load
	 * @return true - success
	 */
	private boolean getEntry(final byte[] dataBuf, final int entryNum) {
		int totalEntries = ((dataBuf[35] & 0xff) << 8) | (dataBuf[34] & 0xff);
		if ((entryNum < 1) || (entryNum > totalEntries)) {
			// entry not available
			return false;
		}

		// determine T64 entry offset
		int pos = 32 /* header */ + 32 * entryNum;

		// expect 1 (Normal tape file)
		if (dataBuf[pos++] != 1)
			return false;
		// expect PRG
		if ((dataBuf[pos++] & DirEntry.BITMASK_FILETYPE) != DirEntry.FILETYPE_PRG) {
			return false;
		}
		// Get start address (or Load address)
		info.loadAddr = (dataBuf[pos++] & 0xff)
				+ ((dataBuf[pos++] & 0xff) << 8);
		
		// Get end address (actual end address in memory, if the file was loaded
		// into a C64).
		info.c64dataLen = (dataBuf[pos++] & 0xff)
				+ ((dataBuf[pos++] & 0xff) << 8);
		info.c64dataLen -= info.loadAddr;
		
		// skip unused
		pos += 2;

		// determine offset of program data
		fileOffset = 0;
		for (int power = 0; power <= 3; power++) {
			fileOffset += (dataBuf[pos++] & 0xff) << 8 * power;
		}

		// skip unused
		pos += 4;
		
		// Get program name
		lastEntryName = new byte[16];
		System.arraycopy(dataBuf, pos, lastEntryName, 0, 16);
		return true;
	}

	public byte[] getLastEntryName() {
		return lastEntryName;
	}
	
	public static Directory getDirectory(File file) throws IOException {
		Directory dir = new Directory();
		final T64 t64 = new T64();

		// Load T64
		final int length = (int) file.length();
		t64.data = new byte[length];
		RandomAccessFile fd = new RandomAccessFile(file, "r");
		fd.readFully(t64.data, 0, t64.data.length);
		fd.close();
		// Get title
		byte[] diskName = new byte[32];
		System.arraycopy(t64.data, 0, diskName, 0, diskName.length);
		dir.setTitle(new String(diskName, ISO88591).toUpperCase().getBytes(
				ISO88591));
		
		int totalEntries = ((t64.data[35] & 0xff) << 8) | (t64.data[34] & 0xff);
		final List<DirEntry> dirEntries = dir.getDirEntries();
		for (int i = 1; i <= totalEntries; i++) {
			if (!t64.getEntry(t64.data, i)) {
				continue;
			}
			final int loadAddr = t64.info.loadAddr;
			final int c64dataLen = t64.info.c64dataLen;
			final int fileOffset = t64.fileOffset;
			
			dirEntries.add(new DirEntry(t64.info.c64dataLen, t64
					.getLastEntryName(), (byte) 0x82) {
				/**
				 * Save the program of this directory entry to the specified
				 * file.
				 * 
				 * @param autostartFile
				 *            file to save
				 * @throws IOException
				 *             File write error
				 */
				@Override
				public void save(final File autostartFile) throws IOException {
					t64.save(autostartFile, loadAddr, c64dataLen, fileOffset);
				}
			});
		}
		return dir;
	}

	public void save(File file, final int loadAddr, final int c64dataLen,
			final int fileOffset) throws IOException {
		DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));
		dout.writeByte(loadAddr & 0xff);
		int hiBytePart = loadAddr & 0xff00;
		dout.writeByte((hiBytePart) >> 8);
		dout.write(data, fileOffset, c64dataLen);
		dout.close();
	}
}
