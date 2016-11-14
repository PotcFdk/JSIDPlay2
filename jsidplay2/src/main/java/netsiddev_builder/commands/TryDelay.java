package netsiddev_builder.commands;

import netsiddev.Command;;

public class TryDelay implements NetSIDPkg {
	private byte sidNum;
	private int cycles;

	public TryDelay(byte sidNum, int cycles) {
		this.sidNum = sidNum;
		this.cycles = cycles;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_DELAY.ordinal(), sidNum, 0, 0, (byte) ((cycles >> 8) & 0xff),
				(byte) (cycles & 0xff) };
	}
}
