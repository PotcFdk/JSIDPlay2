package netsiddev_builder.commands;

import netsiddev.Command;

public class GetVersion implements NetSIDPkg {

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.GET_VERSION.ordinal(), 0, 0, 0 };
	}
}
