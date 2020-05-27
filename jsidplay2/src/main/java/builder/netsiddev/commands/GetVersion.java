package builder.netsiddev.commands;

import static server.netsiddev.Command.GET_VERSION;

public class GetVersion implements NetSIDPkg {

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) GET_VERSION.ordinal(), 0, 0, 0 };
	}
}
