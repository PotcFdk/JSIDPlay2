package builder.netsiddev.commands;

import static server.netsiddev.Command.TRY_SET_SAMPLING;

import libsidplay.common.SamplingMethod;

public class TrySetSampling implements NetSIDPkg {
	private final byte sampling;

	public TrySetSampling(SamplingMethod sampling) {
		this.sampling = (byte) sampling.ordinal();
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_SAMPLING.ordinal(), 0, 0, 0, sampling };
	}
}
