package netsiddev_builder.commands;

import netsiddev.Command;

public class TryReset implements NetSIDPkg {
	private byte sidNum;
	private byte volume;

	public TryReset(byte sidNum, byte volume) {
		this.sidNum = sidNum;
		this.volume = volume;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_RESET.ordinal(), sidNum, 0, 0, volume };
	}
}
