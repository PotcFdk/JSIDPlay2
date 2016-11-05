package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_FLUSH;

public class Flush implements NetSIDPkg {
	private byte sidNum;

	public Flush(byte sidNum) {
		this.sidNum = sidNum;
	}

	public byte[] toByteArray() {
		return new byte[] { CMD_FLUSH.getCmd(), sidNum, 0, 0 };
	}
}
