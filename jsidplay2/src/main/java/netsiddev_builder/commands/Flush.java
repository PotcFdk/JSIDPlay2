package netsiddev_builder.commands;

import netsiddev.Command;

public class Flush implements NetSIDPkg {

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.FLUSH.ordinal(), 0, 0, 0 };
	}
}
