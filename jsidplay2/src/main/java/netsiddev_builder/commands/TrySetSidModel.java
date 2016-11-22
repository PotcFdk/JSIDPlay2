package netsiddev_builder.commands;

import static netsiddev.Command.TRY_SET_SID_MODEL;

public class TrySetSidModel implements NetSIDPkg {
	private byte sidNum;
	private byte config;

	public TrySetSidModel(byte sidNum, byte config) {
		this.sidNum = sidNum;
		this.config = config;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_SID_MODEL.ordinal(), sidNum, 0, 0, config };
	}
}
