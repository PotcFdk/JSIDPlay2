package netsiddev_builder.commands;

import static netsiddev.Command.TRY_SET_SAMPLING;

import libsidplay.common.SamplingMethod;

public class TrySetSampling implements NetSIDPkg {
	private byte sampling;

	public TrySetSampling(SamplingMethod sampling) {
		this.sampling = (byte) sampling.ordinal();
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_SAMPLING.ordinal(), 0, 0, 0, sampling };
	}
}
