package netsiddev_builder.commands;

import netsiddev.Command;

public class SetSIDPosition implements NetSIDPkg {
	private byte sidNum;
	private byte position;

	public SetSIDPosition(byte sidNum, byte position) {
		this.sidNum = sidNum;
		this.position = position;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.SET_SID_POSITION.ordinal(), sidNum, 0, 0, position };
	}
}
