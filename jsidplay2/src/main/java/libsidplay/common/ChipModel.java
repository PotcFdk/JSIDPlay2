package libsidplay.common;

import static libsidplay.sidtune.SidTune.RESET;
import static libsidplay.sidtune.SidTune.Model.UNKNOWN;

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
	 * Detect chip model of specified SID number in the following order:
	 * <OL>
	 * <LI>SID model forced by user configuration
	 * <LI>chip model provided by tune information (auto detected) and if
	 * unknown, then
	 * <LI>default chip model for the 1st SID and for 2nd or 3rd chip use the
	 * same chip model as for the 1st SID
	 * </OL>
	 * 
	 * @return chip model to be used for SID number 0..MAX_SIDS-1
	 */
	public static ChipModel getChipModel(IEmulationSection emulation, SidTune tune, int sidNum) {
		ChipModel forcedChipModel;
		Model tuneSidModel;
		ChipModel defaultSidModel;
		switch (sidNum) {
		case 0:
			forcedChipModel = emulation.getUserSidModel();
			tuneSidModel = tune != RESET ? tune.getInfo().getSIDModel(0) : UNKNOWN;
			defaultSidModel = emulation.getDefaultSidModel();
			break;
		case 1:
			forcedChipModel = emulation.getStereoSidModel();
			tuneSidModel = tune != RESET ? tune.getInfo().getSIDModel(1) : UNKNOWN;
			defaultSidModel = getChipModel(emulation, tune, 0);
			break;
		case 2:
			forcedChipModel = emulation.getThirdSIDModel();
			tuneSidModel = tune != RESET ? tune.getInfo().getSIDModel(2) : UNKNOWN;
			defaultSidModel = getChipModel(emulation, tune, 0);
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		if (forcedChipModel != AUTO) {
			return forcedChipModel;
		}
		if (tuneSidModel == UNKNOWN) {
			return defaultSidModel;
		}
		switch (tuneSidModel) {
		case MOS6581:
		case MOS8580:
			return tuneSidModel.asChipModel();
		default:
			return defaultSidModel;
		}
	}

}