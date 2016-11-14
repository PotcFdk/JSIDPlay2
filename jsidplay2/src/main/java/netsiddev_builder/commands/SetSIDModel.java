package netsiddev_builder.commands;

import netsiddev.Command;

public class SetSIDModel implements NetSIDPkg {
	private byte sidNum;
	private byte config;

	public SetSIDModel(byte sidNum, byte config) {
		this.sidNum = sidNum;
		this.config = config;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_SET_SID_MODEL.ordinal(), sidNum, 0, 0, config };
	}
}
