package netsiddev_builder.commands;

import static netsiddev.Command.TRY_READ;
import static netsiddev.Command.TRY_WRITE;

import java.util.ArrayList;
import java.util.List;

public class TryWrite implements NetSIDPkg {
	private List<Write> writes = new ArrayList<>();
	private int cyclesSentToServer;
	private boolean isRead;
	private byte sidNumToRead;
	private int readCycles;
	private byte readAddr;

	public void addWrite(int cycles, byte reg, byte data) {
		writes.add(new Write(cycles, reg, data));
		cyclesSentToServer += cycles;
	}

	public void toTryRead(byte sidNum, int cycles, byte addr) {
		isRead = true;
		sidNumToRead = sidNum;
		readCycles = cycles;
		readAddr = addr;
		cyclesSentToServer += cycles;
	}

	public int getCyclesSentToServer() {
		return cyclesSentToServer;
	}

	public byte[] toByteArray() {
		int i = 0;
		byte[] cmd = new byte[4/*head.length*/ + (writes.size() << 2) + (isRead ? 3 : 0)];
		cmd[i++] = (byte) (isRead ? TRY_READ : TRY_WRITE).ordinal();
		cmd[i++] = isRead ? sidNumToRead : 0;
		cmd[i++] = 0;
		cmd[i++] = 0;
		for (Write write : writes) {
			cmd[i++] = (byte) ((write.getCycles() >> 8) & 0xff);
			cmd[i++] = (byte) (write.getCycles() & 0xff);
			cmd[i++] = write.getReg();
			cmd[i++] = write.getData();
		}
		if (isRead) {
			cmd[i++] = (byte) ((readCycles >> 8) & 0xff);
			cmd[i++] = (byte) (readCycles & 0xff);
			cmd[i++] = readAddr;
		}
		return cmd;
	}

}
