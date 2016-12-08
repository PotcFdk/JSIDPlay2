package netsiddev_builder.commands;

import static netsiddev.Command.TRY_READ;

public class TryRead extends TryWrite {
	private byte sidNumToRead;
	private int readCycles;
	private byte readAddr;

	public TryRead(TryWrite tryWrite, byte sidNum, int cycles, byte addr) {
		this(sidNum, cycles, addr);
		for (Write write : tryWrite.writes) {
			addWrite(write.getCycles(), write.getReg(), write.getData());
		}
	}

	public TryRead(byte sidNum, int cycles, byte addr) {
		sidNumToRead = sidNum;
		readCycles = cycles;
		readAddr = addr;
		cyclesSendToServer = cycles;
	}

	@Override
	public byte[] toByteArray() {
		int i = 0;
		byte[] cmd = new byte[4 + (writes.size() << 2) + 3];
		cmd[i++] = (byte) TRY_READ.ordinal();
		cmd[i++] = sidNumToRead;
		cmd[i++] = 0;
		cmd[i++] = 0;
		for (Write write : writes) {
			cmd[i++] = (byte) ((write.getCycles() >> 8) & 0xff);
			cmd[i++] = (byte) (write.getCycles() & 0xff);
			cmd[i++] = write.getReg();
			cmd[i++] = write.getData();
		}
		cmd[i++] = (byte) ((readCycles >> 8) & 0xff);
		cmd[i++] = (byte) (readCycles & 0xff);
		cmd[i++] = readAddr;
		return cmd;
	}
}