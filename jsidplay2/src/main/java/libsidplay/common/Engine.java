package libsidplay.common;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;

public enum Engine {
	/** Software (emulation using RESID or RESIDfp) */
	EMULATION,
	/** Software (Network SID Device via socket connection) */
	NETSID,
	/** Hardware (HardSID4U USB device) */
	HARDSID,
	/** Hardware (HardSID4U Java-USB device) */
	JHARDSID,
	/** Hardware (SidBlaster USB device) */
	SIDBLASTER,
	/** Hardware (ExSID USB device) */
	EXSID;

	/**
	 * Choose engine to be used (MP3 requires EMULATION).
	 * 
	 * @return engine to be used
	 */
	public static Engine getEngine(IEmulationSection emulationSection, SidTune tune) {
		if (tune instanceof MP3Tune) {
			return EMULATION;
		}
		return emulationSection.getEngine();
	}
}
