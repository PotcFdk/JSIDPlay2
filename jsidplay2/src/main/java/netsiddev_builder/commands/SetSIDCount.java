package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_TRY_SET_SID_COUNT;

public class SetSIDCount implements NetSIDPkg {
	private byte numSids;

	public SetSIDCount(byte numSids) {
		this.numSids = numSids;
	}

	public byte[] toByteArray() {
		return new byte[] { CMD_TRY_SET_SID_COUNT.getCmd(), numSids, 0, 0 };
	}
}
