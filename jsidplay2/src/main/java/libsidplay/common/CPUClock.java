package libsidplay.common;

import static libsidplay.sidtune.SidTune.RESET;
import static libsidplay.sidtune.SidTune.Clock.UNKNOWN;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;

public enum CPUClock {
	/** PAL region clock frequency and screen refresh */
	PAL(985248.4, 50),
	/** NTSC region clock frequency and screen refresh */
	NTSC(1022727.14, 60);

	private final double cpuFrequency, screenRefresh;

	private CPUClock(double cpuFrequency, double screenRefresh) {
		this.cpuFrequency = cpuFrequency;
		this.screenRefresh = screenRefresh;
	}

	public double getCpuFrequency() {
		return cpuFrequency;
	}

	public double getScreenRefresh() {
		return screenRefresh;
	}

	/**
	 * Detect CPU clock of a specific tune in the following order:
	 * <OL>
	 * <LI>CPU clock forced by user configuration
	 * <LI>CPU clock provided by tune information (auto detected) and if
	 * unknown, then
	 * <LI>default CPU clock
	 * </OL>
	 * 
	 * @return CPU clock to be used for the tune
	 */
	public static CPUClock getCPUClock(IEmulationSection emulation, SidTune tune) {
		CPUClock forcedCPUClock = emulation.getUserClockSpeed();
		Clock tuneCPUClock = tune != RESET ? tune.getInfo().getClockSpeed() : UNKNOWN;
		CPUClock defaultCPUClock = emulation.getDefaultClockSpeed();
		if (forcedCPUClock != null) {
			return forcedCPUClock;
		}
		if (tuneCPUClock == UNKNOWN) {
			return defaultCPUClock;
		}
		switch (tuneCPUClock) {
		case PAL:
		case NTSC:
			return tuneCPUClock.asCPUClock();
		default:
			return defaultCPUClock;
		}
	}
}