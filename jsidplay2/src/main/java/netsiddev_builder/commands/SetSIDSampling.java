package netsiddev_builder.commands;

import netsiddev.Command;

public class SetSIDSampling implements NetSIDPkg {
	private byte sampling;

	public SetSIDSampling(byte sampling) {
		this.sampling = sampling;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_SET_SAMPLING.ordinal(), 0, 0, 0, sampling };
	}
}
