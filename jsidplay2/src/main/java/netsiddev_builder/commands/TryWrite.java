package netsiddev_builder.commands;

import java.util.ArrayList;
import java.util.List;

import netsiddev.Command;

public class TryWrite implements NetSIDPkg {
	private byte sidNum;
	private List<Write> writes = new ArrayList<>();
	private int cyclesSentToServer;

	public TryWrite(byte sidNum) {
		this.sidNum = sidNum;
	}

	public void addWrite(int cycles, byte reg, byte data) {
		writes.add(new Write(cycles, reg, data));
		cyclesSentToServer += cycles;
	}

	public int getCyclesSentToServer() {
		return cyclesSentToServer;
	}

	public byte[] toByteArray() {
		byte[] head = new byte[] { (byte) Command.TRY_WRITE.ordinal(), sidNum, 0, 0 };
		byte[] cmd = new byte[head.length + (writes.size() << 2)];
		System.arraycopy(head, 0, cmd, 0, head.length);
		int i = head.length;
		for (Write write : writes) {
			cmd[i++] = (byte) ((write.getCycles() >> 8) & 0xff);
			cmd[i++] = (byte) (write.getCycles() & 0xff);
			cmd[i++] = write.getReg();
			cmd[i++] = write.getData();
		}
		return cmd;
	}
}
