package libsidplay.common;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public enum Emulation {
	/** Use default emulation */
	DEFAULT,
	/** Dag Lem's resid 1.0 beta */
	RESID,
	/** Antti S. Lankila's resid-fp */
	RESIDFP;

	/**
	 * Detect SID emulation of specified SID number in the following order:
	 * <OL>
	 * <LI>SID emulation forced by user configuration
	 * <LI>default SID emulation
	 * </OL>
	 * 
	 * @return SID emulation to be used for SID number 0..MAX_SIDS-1
	 */
	public static Emulation getEmulation(IEmulationSection emulationSection, SidTune tune, int sidNum) {
		Emulation forcedEmulation;
		Emulation defaultEmulation = emulationSection.getDefaultEmulation();
		switch (sidNum) {
		case 0:
			forcedEmulation = emulationSection.getUserEmulation();
			break;
		case 1:
			forcedEmulation = emulationSection.getStereoEmulation();
			break;
		case 2:
			forcedEmulation = emulationSection.getThirdEmulation();
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		return forcedEmulation != DEFAULT ? forcedEmulation : defaultEmulation;
	}
}