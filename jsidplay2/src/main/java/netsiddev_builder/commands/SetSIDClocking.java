package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.TRY_SET_CLOCKING;

import libsidplay.common.CPUClock;;

public class SetSIDClocking implements NetSIDPkg {
	private byte sidNum;
	private double cpuFrequency;

	public SetSIDClocking(byte sidNum, double cpuFrequency) {
		this.sidNum = sidNum;
		this.cpuFrequency = cpuFrequency;
	}

	public byte[] toByteArray() {
		return new byte[] { TRY_SET_CLOCKING.getCmd(), sidNum, 0, 0,
				(byte) (CPUClock.PAL.getCpuFrequency() == cpuFrequency ? 0 : 1) };
	}
}
