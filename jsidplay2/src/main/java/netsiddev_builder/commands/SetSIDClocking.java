package netsiddev_builder.commands;

import libsidplay.common.CPUClock;
import netsiddev.Command;;

public class SetSIDClocking implements NetSIDPkg {
	private double cpuFrequency;

	public SetSIDClocking(double cpuFrequency) {
		this.cpuFrequency = cpuFrequency;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) Command.TRY_SET_CLOCKING.ordinal(), 0, 0, 0,
				(byte) (CPUClock.PAL.getCpuFrequency() == cpuFrequency ? 0 : 1) };
	}
}
