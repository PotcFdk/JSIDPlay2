package libsidplay.common;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public enum Emulation {
	/** Dag Lem's resid 1.0 beta */
	RESID("Dag Lem's resid 1.0 beta"),
	/** Antti S. Lankila's resid-fp */
	RESIDFP("Antti S. Lankila's resid-fp");

	private final String description;

	Emulation(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}

	/**
	 * Detect SID emulation of specified SID number
	 * <OL>
	 * <LI>forced SID emulation
	 * <LI>default SID emulaton or use 1st SID emulation (for 2nd or 3rd chip)
	 * </OL>
	 * 
	 * @return SID emulation to be used for SID number
	 */
	public static Emulation getEmulation(IEmulationSection emulationSection,
			SidTune tune, int sidNum) {
		Emulation forcedEmulation;
		Emulation defaultEmulation;
		switch (sidNum) {
		case 0:
			forcedEmulation = emulationSection.getUserEmulation();
			defaultEmulation = emulationSection.getDefaultEmulation();
			break;
		case 1:
			forcedEmulation = emulationSection.getStereoEmulation();
			defaultEmulation = getEmulation(emulationSection, tune, 0);
			break;
		case 2:
			forcedEmulation = emulationSection.getThirdEmulation();
			defaultEmulation = getEmulation(emulationSection, tune, 0);
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		return forcedEmulation != null ? forcedEmulation : defaultEmulation;
	}
}