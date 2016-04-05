package libsidutils.prg2tap;

import java.nio.charset.Charset;

import libsidplay.sidtune.SidTune;

public class PRG2TAPProgram {
	/**
	 * Char-set for string to byte conversions.
	 */
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");

	private static final int MAX_MEM_SIZE = 65536;

	private final byte[] name = new byte[PRG2TAP.MAX_NAME_LENGTH];
	private final int startAddr;
	private final int length;
	private final byte[] mem = new byte[MAX_MEM_SIZE];

	public PRG2TAPProgram(SidTune sidTune, String name) {
		sidTune.placeProgramInMemory(mem);
		startAddr = sidTune.getInfo().getLoadAddr();
		length = sidTune.getInfo().getC64dataLen();
		final byte[] petscii = filenameTopetscii(name, 16);
		System.arraycopy(petscii, 0, this.name, 0, petscii.length);
	}

	public final static byte[] filenameTopetscii(final String str, int maxLen) {
		return str.substring(0, Math.min(maxLen, str.length())).toUpperCase().replace('_', '-').getBytes(ISO88591);
	}

	public byte[] getName() {
		return name;
	}

	public int getStartAddr() {
		return startAddr;
	}

	public int getLength() {
		return length;
	}

	public byte[] getMem() {
		return mem;
	}

}