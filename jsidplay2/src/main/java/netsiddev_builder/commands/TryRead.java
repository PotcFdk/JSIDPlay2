package netsiddev_builder.commands;

import static netsiddev.Command.TRY_READ;

public class TryRead implements NetSIDPkg {
	private byte sidNum;
	private byte cyclesHigh, cyclesLow;
	private byte register;

	public TryRead(byte sidNum, int cycles, byte register) {
		this.sidNum = sidNum;
		this.cyclesHigh = (byte) ((cycles >> 8) & 0xff);
		this.cyclesLow = (byte) (cycles & 0xff);
		this.register = register;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_READ.ordinal(), sidNum, 0, 0, cyclesHigh, cyclesLow, register };
	}
}
