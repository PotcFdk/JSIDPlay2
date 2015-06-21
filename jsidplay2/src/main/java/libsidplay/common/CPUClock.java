package libsidplay.common;

import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;

public enum CPUClock {
	PAL(985248.4, 50), NTSC(1022727.14, 60);

	private final double frequency;
	private final double refresh;

	CPUClock(double frequency, double refresh) {
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
	public static CPUClock getCPUClock(IConfig config, SidTune tune) {
		IEmulationSection emulation = config.getEmulationSection();
		SidTuneInfo tuneInfo = tune != null ? tune.getInfo() : null;
		CPUClock cpuFreq = emulation.getUserClockSpeed();
		if (cpuFreq == null) {
			cpuFreq = emulation.getDefaultClockSpeed();
			if (tuneInfo != null) {
				switch (tuneInfo.getClockSpeed()) {
				case UNKNOWN:
				case ANY:
					cpuFreq = emulation.getDefaultClockSpeed();
					break;
				case PAL:
				case NTSC:
					cpuFreq = CPUClock.valueOf(tuneInfo.getClockSpeed()
							.toString());
					break;
				}
			}
		}
		return cpuFreq;
	}
}