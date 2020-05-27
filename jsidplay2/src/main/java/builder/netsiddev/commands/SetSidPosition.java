package builder.netsiddev.commands;

import static server.netsiddev.Command.SET_SID_POSITION;

public class SetSidPosition implements NetSIDPkg {
	private final byte sidNum;
	private final byte position;

	public SetSidPosition(byte sidNum, byte balance) {
		this.sidNum = sidNum;
		this.position = balance;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) SET_SID_POSITION.ordinal(), sidNum, 0, 0, position };
	}
}
