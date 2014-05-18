package libsidutils;

import libsidplay.components.DirEntry;
import libsidplay.sidtune.SidTune;

public class PRG2TAPProgram {
	private static final int MAX_MEM_SIZE = 65536;

	private final byte[] name = new byte[PRG2TAP.MAX_NAME_LENGTH];
	private final int startAddr;
	private final int length;
	private final byte[] mem = new byte[MAX_MEM_SIZE];

	public PRG2TAPProgram(SidTune sidTune, String name) {
		sidTune.placeProgramInMemory(mem);
		startAddr = sidTune.getInfo().getLoadAddr();
		length = sidTune.getInfo().getC64dataLen();
		final byte[] petscii = DirEntry.asciiTopetscii(name, 16);
		System.arraycopy(petscii, 0, this.name, 0, petscii.length);
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