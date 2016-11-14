package netsiddev_builder.commands;

import netsiddev.Command;

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
		return new byte[] { (byte) Command.TRY_READ.ordinal(), sidNum, 0, 0, (byte) ((cycles >> 8) & 0xff),
				(byte) (cycles & 0xff), register };
	}
}
