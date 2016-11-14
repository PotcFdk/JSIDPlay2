package netsiddev_builder.commands;

import netsiddev.Command;

public class SetSIDCount implements NetSIDPkg {
	private byte numSids;

	public SetSIDCount(byte numSids) {
		this.numSids = numSids;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_SET_SID_COUNT.ordinal(), numSids, 0, 0 };
	}
}
