package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_TRY_DELAY;;

public class TryDelay implements NetSIDPkg {
	private byte sidNum;
	private int cycles;

	public TryDelay(byte sidNum, int cycles) {
		this.sidNum = sidNum;
		this.cycles = cycles;
	}

	public byte[] toByteArray() {
		return new byte[] { CMD_TRY_DELAY.getCmd(), sidNum, 0, 0, (byte) ((cycles >> 8) & 0xff),
				(byte) (cycles & 0xff) };
	}
}
