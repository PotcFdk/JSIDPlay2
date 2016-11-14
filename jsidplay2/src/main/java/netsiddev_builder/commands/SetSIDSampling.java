package netsiddev_builder.commands;

import netsiddev.Command;

public class SetSIDSampling implements NetSIDPkg {
	private byte sidNum;
	private byte sampling;

	public SetSIDSampling(byte sidNum, byte sampling) {
		this.sidNum = sidNum;
		this.sampling = sampling;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_SET_SAMPLING.ordinal(), sidNum, 0, 0, sampling };
	}
}
