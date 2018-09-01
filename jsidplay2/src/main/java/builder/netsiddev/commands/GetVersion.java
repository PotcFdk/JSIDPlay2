package builder.netsiddev.commands;

import static netsiddev.Command.GET_VERSION;

public class GetVersion implements NetSIDPkg {

	public byte[] toByteArray() {
		return new byte[] { (byte) GET_VERSION.ordinal(), 0, 0, 0 };
	}
}
