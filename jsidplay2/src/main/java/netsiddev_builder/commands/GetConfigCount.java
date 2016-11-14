package netsiddev_builder.commands;

import netsiddev.Command;

public class GetConfigCount implements NetSIDPkg {

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.GET_CONFIG_COUNT.ordinal(), 0, 0, 0 };
	}
}
