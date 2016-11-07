package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_TRY_READ;

public class TryRead implements NetSIDPkg {
	private byte sidNum;
	private int cycles;
	private byte register;

	public TryRead(byte sidNum, int cycles, byte register) {
		this.sidNum = sidNum;
		this.cycles = cycles;
		this.register = register;
	}

	public byte[] toByteArray() {
		return new byte[] { CMD_TRY_READ.getCmd(), sidNum, 0, 0, (byte) ((cycles >> 8) & 0xff),
				(byte) (cycles & 0xff), register };
	}
}
