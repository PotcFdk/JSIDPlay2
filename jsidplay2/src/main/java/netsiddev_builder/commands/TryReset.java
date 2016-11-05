package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_TRY_RESET;

public class TryReset implements NetSIDPkg {
	private byte sidNum;
	private byte volume;

	public TryReset(byte sidNum, byte volume) {
		this.sidNum = sidNum;
		this.volume = volume;
	}

	public byte[] toByteArray() {
		return new byte[] { CMD_TRY_RESET.getCmd(), sidNum, 0, 0, volume };
	}
}
