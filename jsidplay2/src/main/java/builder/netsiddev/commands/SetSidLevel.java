package builder.netsiddev.commands;

import static netsiddev.Command.SET_SID_LEVEL;

public class SetSidLevel implements NetSIDPkg {
	private final byte sidNum;
	private final byte volume;

	public SetSidLevel(byte sidNum, byte volume) {
		this.sidNum = sidNum;
		this.volume = volume;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) SET_SID_LEVEL.ordinal(), sidNum, 0, 0, volume };
	}
}
