package builder.netsiddev.commands;

import static server.netsiddev.Command.TRY_SET_SID_COUNT;

public class TrySetSidCount implements NetSIDPkg {
	private final byte numSids;

	public TrySetSidCount(byte numSids) {
		this.numSids = numSids;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_SID_COUNT.ordinal(), numSids, 0, 0 };
	}
}
