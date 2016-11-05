package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.SET_SID_POSITION;

public class SetSIDPosition implements NetSIDPkg {
	private byte sidNum;
	private byte position;

	public SetSIDPosition(byte sidNum, byte position) {
		this.sidNum = sidNum;
		this.position = position;
	}

	public byte[] toByteArray() {
		return new byte[] { SET_SID_POSITION.getCmd(), sidNum, 0, 0, position };
	}
}
