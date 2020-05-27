package builder.netsiddev.commands;

import static server.netsiddev.Command.GET_CONFIG_INFO;

public class GetConfigInfo implements NetSIDPkg {
	private final byte sidNum;

	public GetConfigInfo(byte sidNum) {
		this.sidNum = sidNum;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) GET_CONFIG_INFO.ordinal(), sidNum, 0, 0 };
	}
}
