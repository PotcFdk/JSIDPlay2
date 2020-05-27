package builder.netsiddev.commands;

import static server.netsiddev.Command.TRY_RESET;

public class TryReset implements NetSIDPkg {
	private final byte volume;

	public TryReset(byte volume) {
		this.volume = volume;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_RESET.ordinal(), 0, 0, 0, volume };
	}
}
