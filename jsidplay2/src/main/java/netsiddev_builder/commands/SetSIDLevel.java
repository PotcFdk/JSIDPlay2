package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.SET_SID_LEVEL;

public class SetSIDLevel implements NetSIDPkg {
	private byte sidNum;
	private byte levelDb;

	public SetSIDLevel(byte sidNum, byte levelDb) {
		this.sidNum = sidNum;
		this.levelDb = levelDb;
	}

	public byte[] toByteArray() {
		return new byte[] { SET_SID_LEVEL.getCmd(), sidNum, 0, 0, levelDb };
	}
}
