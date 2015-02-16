package libsidplay.common;

import libsidplay.sidtune.SidTune;
import sidplay.ini.intf.IEmulationSection;

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