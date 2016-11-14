package netsiddev_builder.commands;

import netsiddev.Command;

public class GetConfigInfo implements NetSIDPkg {
	private byte sidNum;

	public GetConfigInfo(byte sidNum) {
		this.sidNum = sidNum;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.GET_CONFIG_INFO.ordinal(), sidNum, 0, 0 };
	}
}
