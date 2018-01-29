package libsidplay.sidtune;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import libsidutils.PathUtils;
import libsidutils.Petscii;

public class T64 extends Prg {
	/**
	 * BITMASK_FILETYPE.
	 */
	public static final byte BITMASK_FILETYPE = (byte) 0x7;
	/**
	 * FILETYPE_PRG.
	 */
	public static final byte FILETYPE_PRG = (byte) 0x02;

	public static final class T64Entry {
		public int programOffset;
		public int loadAddr;
		public int c64dataLen;
		public byte[] name;
	}

	protected static SidTune load(final String name, final byte[] dataBuf) throws SidTuneError {
		if (!PathUtils.getFilenameSuffix(name).equalsIgnoreCase(".t64")) {
			throw new SidTuneError("T64: Bad file extension expected: .t64");
		}
		final T64 t64 = new T64();
		final T64Entry entry = t64.getEntry(dataBuf, 1);
		t64.program = dataBuf;
		t64.programOffset = entry.programOffset;
		t64.info.loadAddr = entry.loadAddr;
		// don't trust entry.c64dataLen
		t64.info.c64dataLen = Math.min(entry.c64dataLen, dataBuf.length - t64.programOffset);

		final String credit = Petscii.petsciiToIso88591(entry.name);
		t64.info.infoString.add(credit);

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
	public T64Entry getEntry(final byte[] dataBuf, final int entryNum) throws SidTuneError {
		int totalEntries = ((dataBuf[35] & 0xff) << 8) | (dataBuf[34] & 0xff);
		if (entryNum < 1 || entryNum > totalEntries) {
			throw new SidTuneError("T64: Illegal T64 entry number: " + entryNum + ", must be 1.." + totalEntries);
		}
		int pos = 32 /* header */ + 32 * entryNum;
		// expect 1 (Normal tape file) and PRG
		final byte type = dataBuf[pos++];
		final byte fileType = dataBuf[pos++];
		if (pos + 32 > dataBuf.length || (type != 1 && (fileType & BITMASK_FILETYPE) != FILETYPE_PRG)) {
			throw new SidTuneError("T64: Illegal T64 entry type, must be PRG normal tape file");
		}
		final T64Entry entry = new T64Entry();
		// Get start address (or Load address)
		entry.loadAddr = (dataBuf[pos++] & 0xff) + ((dataBuf[pos++] & 0xff) << 8);

		// Get end address (actual end address in memory, if the file was loaded
		// into a C64).
		entry.c64dataLen = (dataBuf[pos++] & 0xff) + ((dataBuf[pos++] & 0xff) << 8);
		entry.c64dataLen -= entry.loadAddr;

		// skip unused
		pos += 2;

		// determine offset of program data
		entry.programOffset = 0;
		for (int i = 0; i <= 3; i++) {
			entry.programOffset += (dataBuf[pos++] & 0xff) << (8 * i);
		}

		// skip unused
		pos += 4;

		// Get program name
		entry.name = new byte[16];
		System.arraycopy(dataBuf, pos, entry.name, 0, entry.name.length);
		return entry;
	}

	public void save(File file, byte[] program, final int programOffset, final int c64dataLen, final int loadAddr)
			throws IOException {
		try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(file))) {
			dout.writeByte(loadAddr & 0xff);
			dout.writeByte((loadAddr >> 8) & 0xff);
			dout.write(program, programOffset, c64dataLen);
		}
	}
}
