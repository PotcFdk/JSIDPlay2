package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_TRY_WRITE;

import java.util.ArrayList;
import java.util.List;

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
		byte[] head = new byte[] { CMD_TRY_WRITE.getCmd(), sidNum, 0, 0 };
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
