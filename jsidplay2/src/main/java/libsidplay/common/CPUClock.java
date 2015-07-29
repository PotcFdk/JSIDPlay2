package libsidplay.common;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;

public enum CPUClock {
	/** PAL region clock frequency and screen refresh */
	PAL(985248.4, 50),
	/** NTSC region clock frequency and screen refresh */
	NTSC(1022727.14, 60);

	private final double frequency;
	private final double refresh;

	private CPUClock(double frequency, double refresh) {
		this.frequency = frequency;
		this.refresh = refresh;
	}

	public double getCpuFrequency() {
		return frequency;
	}

	public double getCyclesPerFrame() {
		return frequency / refresh;
	}

	public double getRefresh() {
		return refresh;
	}

	/**
	 * Detect CPU clock of specific tune.
	 * <OL>
	 * <LI>forced CPU clock
	 * <LI>CPU clock provided by tune information
	 * <LI>default CPU clock
	 * </OL>
	 * 
	 * @return CPU clock to be used for the tune
	 */
	public static CPUClock getCPUClock(IEmulationSection emulation, SidTune tune) {
		CPUClock forcedCPUClock = emulation.getUserClockSpeed();
		Clock tuneCPUClock = tune != null ? tune.getInfo().getClockSpeed()
				: null;
		CPUClock defaultCPUClock = emulation.getDefaultClockSpeed();
		if (forcedCPUClock != null) {
			return forcedCPUClock;
		}
		if (tuneCPUClock == null) {
			return defaultCPUClock;
		}
		switch (tuneCPUClock) {
		case PAL:
		case NTSC:
			return CPUClock.valueOf(tuneCPUClock.toString());
		default:
			return defaultCPUClock;
		}
	}
}