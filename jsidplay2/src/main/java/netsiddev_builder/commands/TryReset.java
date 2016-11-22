package netsiddev_builder.commands;

import static netsiddev.Command.TRY_RESET;

public class TryReset implements NetSIDPkg {
	private byte volume;

	public TryReset(byte volume) {
		this.volume = volume;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_RESET.ordinal(), 0, 0, 0, volume };
	}
}
