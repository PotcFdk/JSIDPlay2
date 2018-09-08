package builder.netsiddev.commands;

import static server.netsiddev.Command.TRY_WRITE;

import java.util.ArrayList;
import java.util.List;

public class TryWrite implements NetSIDPkg {

	protected final List<Write> writes = new ArrayList<>();
	protected int cyclesSendToServer;

	public void addWrite(int cycles, byte reg, byte data) {
		writes.add(new Write(cycles, reg, data));
		cyclesSendToServer += cycles;
	}

	public int getCyclesSendToServer() {
		return cyclesSendToServer;
	}

	public byte[] toByteArray() {
		int i = 0;
		byte[] cmd = new byte[4 + (writes.size() << 2)];
		cmd[i++] = (byte) TRY_WRITE.ordinal();
		cmd[i++] = 0;
		cmd[i++] = 0;
		cmd[i++] = 0;
		for (Write write : writes) {
			cmd[i++] = (byte) ((write.getCycles() >> 8) & 0xff);
			cmd[i++] = (byte) (write.getCycles() & 0xff);
			cmd[i++] = write.getReg();
			cmd[i++] = write.getData();
		}
		return cmd;
	}

}
