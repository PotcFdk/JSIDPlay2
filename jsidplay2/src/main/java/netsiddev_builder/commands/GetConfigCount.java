package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.GET_CONFIG_COUNT;

public class GetConfigCount implements NetSIDPkg {

	public byte[] toByteArray() {
		return new byte[] { GET_CONFIG_COUNT.getCmd(), 0, 0, 0 };
	}
}
