package netsiddev_builder.commands;

import static netsiddev.Command.SET_SID_LEVEL;

public class SetSidLevel implements NetSIDPkg {
	private byte sidNum;
	private byte levelDb;

	public SetSidLevel(byte sidNum, byte levelDb) {
		this.sidNum = sidNum;
		this.levelDb = levelDb;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) SET_SID_LEVEL.ordinal(), sidNum, 0, 0, levelDb };
	}
}
