package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.GET_CONFIG_INFO;

public class GetConfigInfo implements NetSIDPkg {
	private byte sidNum;

	public GetConfigInfo(byte sidNum) {
		this.sidNum = sidNum;
	}

	public byte[] toByteArray() {
		return new byte[] { GET_CONFIG_INFO.getCmd(), sidNum, 0, 0 };
	}
}
