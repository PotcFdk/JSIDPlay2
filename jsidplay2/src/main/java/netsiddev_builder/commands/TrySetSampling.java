package netsiddev_builder.commands;

import static netsiddev.Command.TRY_SET_SAMPLING;

public class TrySetSampling implements NetSIDPkg {
	private byte sampling;

	public TrySetSampling(byte sampling) {
		this.sampling = sampling;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_SAMPLING.ordinal(), 0, 0, 0, sampling };
	}
}
