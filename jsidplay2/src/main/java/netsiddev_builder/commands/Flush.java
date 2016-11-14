package netsiddev_builder.commands;

import netsiddev.Command;

public class Flush implements NetSIDPkg {
	private byte sidNum;

	public Flush(byte sidNum) {
		this.sidNum = sidNum;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.FLUSH.ordinal(), sidNum, 0, 0 };
	}
}
