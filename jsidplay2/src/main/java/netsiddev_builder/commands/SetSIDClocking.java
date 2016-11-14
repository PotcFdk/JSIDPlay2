package netsiddev_builder.commands;

import libsidplay.common.CPUClock;
import netsiddev.Command;;

public class SetSIDClocking implements NetSIDPkg {
	private byte sidNum;
	private double cpuFrequency;

	public SetSIDClocking(byte sidNum, double cpuFrequency) {
		this.sidNum = sidNum;
		this.cpuFrequency = cpuFrequency;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_SET_CLOCKING.ordinal(), sidNum, 0, 0,
				(byte) (CPUClock.PAL.getCpuFrequency() == cpuFrequency ? 0 : 1) };
	}
}
