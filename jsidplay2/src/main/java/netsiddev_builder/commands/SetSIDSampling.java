package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.TRY_SET_SAMPLING;

public class SetSIDSampling implements NetSIDPkg {
	private byte sidNum;
	private byte sampling;

	public SetSIDSampling(byte sidNum, byte sampling) {
		this.sidNum = sidNum;
		this.sampling = sampling;
	}

	public byte[] toByteArray() {
		return new byte[] { TRY_SET_SAMPLING.getCmd(), sidNum, 0, 0, sampling };
	}
}
