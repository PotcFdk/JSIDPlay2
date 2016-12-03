package netsiddev_builder.commands;

import static netsiddev.Command.TRY_READ;
import static netsiddev.Command.TRY_WRITE;

import java.util.ArrayList;
import java.util.List;

public class TryWrite implements NetSIDPkg {

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
	
	protected List<Write> writes = new ArrayList<>();
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
