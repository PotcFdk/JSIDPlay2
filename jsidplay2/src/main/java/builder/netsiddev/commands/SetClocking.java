package builder.netsiddev.commands;

import static libsidplay.common.CPUClock.PAL;
import static server.netsiddev.Command.TRY_SET_CLOCKING;

public class SetClocking implements NetSIDPkg {
	private final byte cpuFrequency;

	public SetClocking(double cpuFrequency) {
		this.cpuFrequency = (byte) (PAL.getCpuFrequency() == cpuFrequency ? 0 : 1);
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_CLOCKING.ordinal(), 0, 0, 0, cpuFrequency };
	}
}
