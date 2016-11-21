package netsiddev_builder.commands;

import netsiddev.Command;

public class TryReset implements NetSIDPkg {
	private byte volume;

	public TryReset(byte volume) {
		this.volume = volume;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_RESET.ordinal(), 0, 0, 0, volume };
	}
}
