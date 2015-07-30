package libsidplay.common;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;

public enum ChipModel {
	/** Auto-detect SID model */
	AUTO,
	/** SID chip of the old C64 */
	MOS6581,
	/** SID chip of the new C64 */
	MOS8580;

	/**
	 * Detect chip model of specified SID number
	 * <OL>
	 * <LI>forced chip model
	 * <LI>chip model provided by tune information
	 * <LI>default chip model or use 1st chip model (for 2nd or 3rd chip)
	 * </OL>
	 * 
	 * @return chip model to be used for SID number
	 */
	public static ChipModel getChipModel(IEmulationSection emulation,
			SidTune tune, int sidNum) {
		ChipModel forcedChipModel;
		Model tuneSidModel;
		ChipModel defaultSidModel;
		switch (sidNum) {
		case 0:
			forcedChipModel = emulation.getUserSidModel();
			tuneSidModel = tune != null ? tune.getInfo().getSid1Model() : null;
			defaultSidModel = emulation.getDefaultSidModel();
			break;
		case 1:
			forcedChipModel = emulation.getStereoSidModel();
			tuneSidModel = tune != null ? tune.getInfo().getSid2Model() : null;
			defaultSidModel = getChipModel(emulation, tune, 0);
			break;
		case 2:
			forcedChipModel = emulation.getThirdSIDModel();
			tuneSidModel = tune != null ? tune.getInfo().getSid3Model() : null;
			defaultSidModel = getChipModel(emulation, tune, 0);
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		if (forcedChipModel != AUTO) {
			return forcedChipModel;
		}
		if (tuneSidModel == null) {
			return defaultSidModel;
		}
		switch (tuneSidModel) {
		case MOS6581:
		case MOS8580:
			return valueOf(tuneSidModel.toString());
		default:
			return defaultSidModel;
		}
	}

}