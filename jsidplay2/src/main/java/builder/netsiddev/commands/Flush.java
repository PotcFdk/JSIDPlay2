package builder.netsiddev.commands;

import static server.netsiddev.Command.FLUSH;

public class Flush implements NetSIDPkg {

	public byte[] toByteArray() {
		return new byte[] { (byte) FLUSH.ordinal(), 0, 0, 0 };
	}
}
