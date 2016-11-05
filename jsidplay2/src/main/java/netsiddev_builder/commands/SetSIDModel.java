package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.TRY_SET_SID_MODEL;

public class SetSIDModel implements NetSIDPkg {
	private byte sidNum;
	private byte config;

	public SetSIDModel(byte sidNum, byte config) {
		this.sidNum = sidNum;
		this.config = config;
	}

	public byte[] toByteArray() {
		return new byte[] { TRY_SET_SID_MODEL.getCmd(), sidNum, 0, 0, config };
	}
}
