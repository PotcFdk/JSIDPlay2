package netsiddev_builder.commands;

import netsiddev.Command;

public class SetSIDLevel implements NetSIDPkg {
	private byte sidNum;
	private byte levelDb;

	public SetSIDLevel(byte sidNum, byte levelDb) {
		this.sidNum = sidNum;
		this.levelDb = levelDb;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.SET_SID_LEVEL.ordinal(), sidNum, 0, 0, levelDb };
	}
}
