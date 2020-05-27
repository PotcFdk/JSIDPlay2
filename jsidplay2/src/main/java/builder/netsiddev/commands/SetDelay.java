package builder.netsiddev.commands;

import static server.netsiddev.Command.SET_DELAY;

public class SetDelay implements NetSIDPkg {
	private final byte sidNum;
	private final byte delay;

	public SetDelay(byte sidNum, byte delay) {
		this.sidNum = sidNum;
		this.delay = delay;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) SET_DELAY.ordinal(), sidNum, 0, 0, delay };
	}
}
