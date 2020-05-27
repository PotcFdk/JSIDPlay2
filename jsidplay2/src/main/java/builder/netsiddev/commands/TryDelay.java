package builder.netsiddev.commands;

import static server.netsiddev.Command.TRY_DELAY;

public class TryDelay implements NetSIDPkg {
	private final byte sidNum;
	private final byte cyclesHigh, cyclesLow;

	public TryDelay(byte sidNum, int cycles) {
		this.sidNum = sidNum;
		this.cyclesHigh = (byte) (cycles >> 8 & 0xff);
		this.cyclesLow = (byte) (cycles & 0xff);
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_DELAY.ordinal(), sidNum, 0, 0, cyclesHigh, cyclesLow };
	}
}
