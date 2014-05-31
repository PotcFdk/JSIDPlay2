package libsidplay.sidtune;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import libsidplay.components.DirEntry;
import libsidplay.components.Directory;
import libsidutils.PathUtils;

public class T64 extends Prg {

	private static final class T64Entry {
		private int programOffset;
		private int loadAddr;
		private int c64dataLen;
		private byte[] name;
	}

	protected static SidTune load(final String name, final byte[] dataBuf)
			throws SidTuneError {
		if (!PathUtils.getExtension(name).equalsIgnoreCase(".t64")) {
			throw new SidTuneError("Bad file extension expected: .t64");
		}
		final T64 t64 = new T64();
		final T64Entry entry = t64.getEntry(dataBuf, 1);

		t64.program = dataBuf;
		t64.programOffset = entry.programOffset;
		t64.info.loadAddr = entry.loadAddr;
		t64.info.c64dataLen = entry.c64dataLen;

		t64.info.infoString.add(PathUtils.getBaseNameNoExt(name));
		
		t64.convertOldStyleSpeedToTables(~0);
		return t64;
	}

	/**
	 * Load T64 file entry from buffer.
	 * 
	 * @param dataBuf
	 *            buffer with file data
	 * @param entryNum
	 *            entry number to load
	 */
	private T64Entry getEntry(final byte[] dataBuf, final int entryNum)
			throws SidTuneError {
		int totalEntries = ((dataBuf[35] & 0xff) << 8) | (dataBuf[34] & 0xff);
		if (entryNum < 1 || entryNum > totalEntries) {
			throw new SidTuneError("Illegal T64 entry number: " + entryNum
					+ ", must be 1.." + totalEntries);
		}
		int pos = 32 /* header */+ 32 * entryNum;
		// expect 1 (Normal tape file) and PRG
		if (pos + 32 > dataBuf.length
				|| dataBuf[pos++] != 1
				|| (dataBuf[pos++] & DirEntry.BITMASK_FILETYPE) != DirEntry.FILETYPE_PRG) {
			throw new SidTuneError(
					"Illegal T64 entry type, must be PRG normal tape file");
		}
		final T64Entry entry = new T64Entry();
		// Get start address (or Load address)
		entry.loadAddr = (dataBuf[pos++] & 0xff)
				+ ((dataBuf[pos++] & 0xff) << 8);

		// Get end address (actual end address in memory, if the file was loaded
		// into a C64).
		entry.c64dataLen = (dataBuf[pos++] & 0xff)
				+ ((dataBuf[pos++] & 0xff) << 8);
		entry.c64dataLen -= info.loadAddr;

		// skip unused
		pos += 2;

		// determine offset of program data
		entry.programOffset = 0;
		for (int i = 0; i <= 3; i++) {
			entry.programOffset += (dataBuf[pos++] & 0xff) << 8 * i;
		}

		// skip unused
		pos += 4;

		// Get program name
		entry.name = new byte[16];
		System.arraycopy(dataBuf, pos, entry.name, 0, entry.name.length);
		return entry;
	}

	public static Directory getDirectory(File file) throws IOException {
		Directory dir = new Directory();
		final T64 t64 = new T64();

		// Load T64
		byte[] data = new byte[(int) file.length()];
		try (DataInputStream fd = new DataInputStream(new FileInputStream(file))) {
			fd.readFully(data);
			// Get title
			byte[] diskName = new byte[32];
			System.arraycopy(data, 0, diskName, 0, diskName.length);
			dir.setTitle(diskName);

			int totalEntries = ((data[35] & 0xff) << 8) | (data[34] & 0xff);
			final Collection<DirEntry> dirEntries = dir.getDirEntries();
			for (int entryNum = 1; entryNum <= totalEntries; entryNum++) {
				try {
					final T64Entry entry = t64.getEntry(data, entryNum);
					dirEntries.add(new DirEntry(entry.c64dataLen, entry.name,
							(byte) 0x82) {
						@Override
						public void save(final File autostartFile)
								throws IOException {
							t64.save(autostartFile, data, entry.programOffset,
									entry.c64dataLen, entry.loadAddr);
						}
					});
				} catch (SidTuneError e) {
				}
			}
			return dir;
		}
	}

	public void save(File file, byte[] program, final int programOffset,
			final int c64dataLen, final int loadAddr) throws IOException {
		try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(
				file))) {
			dout.writeByte(loadAddr & 0xff);
			dout.writeByte((loadAddr >> 8) & 0xff);
			dout.write(program, programOffset, c64dataLen);
		}
	}
}
