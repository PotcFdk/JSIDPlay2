package netsiddev_builder.commands;

import static netsiddev.Command.SET_SID_POSITION;

public class SetSidPosition implements NetSIDPkg {
	private byte sidNum;
	private byte position;

	public SetSidPosition(byte sidNum, byte position) {
		this.sidNum = sidNum;
		this.position = position;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) SET_SID_POSITION.ordinal(), sidNum, 0, 0, position };
	}
}
