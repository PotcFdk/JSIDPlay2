package libsidplay.common;

import static libsidplay.sidtune.SidTune.RESET;
import static libsidplay.sidtune.SidTune.Clock.UNKNOWN;

import libsidplay.components.mos656x.MOS6567;
import libsidplay.components.mos656x.MOS6569;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;

public enum CPUClock {
	/** Auto-detect clock frequency */
	AUTO(),
	/** PAL region clock frequency and screen refresh parameters */
	PAL(985248.4, MOS6569.CYCLES_PER_LINE, MOS6569.MAX_RASTERS),
	/** NTSC region clock frequency and screen refresh parameters */
	NTSC(1022727.14, MOS6567.CYCLES_PER_LINE, MOS6567.MAX_RASTERS);

	private final double cpuFrequency, screenRefresh;
	private final int cyclesPerFrame;

	private CPUClock() {
		this.cpuFrequency = 0;
		this.cyclesPerFrame = 0;
		this.screenRefresh = 0;
	}

	private CPUClock(double cpuFrequency, int cyclesPerLine, int maxRasters) {
		this.cpuFrequency = cpuFrequency;
		this.cyclesPerFrame = cyclesPerLine * maxRasters;
		this.screenRefresh = cpuFrequency / cyclesPerFrame;
	}

	public double getCpuFrequency() {
		return cpuFrequency;
	}

	public int getCyclesPerFrame() {
		return cyclesPerFrame;
	}

	public double getScreenRefresh() {
		return screenRefresh;
	}

	/**
	 * Detect CPU clock of a specific tune in the following order:
	 * <OL>
	 * <LI>CPU clock forced by user configuration
	 * <LI>CPU clock provided by tune information and if unknown, then
	 * <LI>default CPU clock
	 * </OL>
	 *
	 * @return CPU clock to be used for the tune
	 */
	public static CPUClock getCPUClock(IEmulationSection emulation, SidTune tune) {
		CPUClock forcedCPUClock = emulation.getUserClockSpeed();
		Clock tuneCPUClock = tune != RESET ? tune.getInfo().getClockSpeed() : UNKNOWN;
		CPUClock defaultCPUClock = emulation.getDefaultClockSpeed();
		if (forcedCPUClock != AUTO) {
			return forcedCPUClock;
		}
		switch (tuneCPUClock) {
		case PAL:
		case NTSC:
			return tuneCPUClock.asCPUClock();
		case UNKNOWN:
		default:
			return defaultCPUClock;
		}
	}
}