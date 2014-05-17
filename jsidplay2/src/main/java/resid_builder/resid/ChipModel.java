package resid_builder.resid;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

public enum ChipModel {
	MOS6581, MOS8580;

	/**
	 * Select Mono chip model by configuration and tune
	 * <OL>
	 * <LI>user locked model
	 * <LI>model provided by tune information
	 * <LI>default model
	 * </OL>
	 * 
	 * @return chip model to be used for Mono SID
	 */
	public static ChipModel getChipModel(IConfig config, SidTune tune) {
		IEmulationSection emulation = config.getEmulation();
		ChipModel chipModel = emulation.getUserSidModel();
		if (chipModel == null) {
			chipModel = emulation.getDefaultSidModel();
			SidTuneInfo tuneInfo = tune != null ? tune.getInfo() : null;
			if (tuneInfo != null) {
				switch (tuneInfo.getSid1Model()) {
				case MOS6581:
				case MOS8580:
					chipModel = ChipModel
							.valueOf(tuneInfo.getSid1Model().toString());
					break;
				default:
					break;
				}
			}
		}
		return chipModel;
	}

	/**
	 * Select Stereo chip model by configuration and tune
	 * <OL>
	 * <LI>user locked defined stereo model
	 * <LI>model provided by tune information
	 * <LI>same model as Mono SID
	 * </OL>
	 * 
	 * @return chip model to be used for Stereo SID
	 */
	public static ChipModel getStereoModel(IConfig config, SidTune tune) {
		IEmulationSection emulation = config.getEmulation();
		ChipModel chipModel = emulation.getStereoSidModel();
		if (chipModel == null) {
			chipModel = getChipModel(config, tune);
			SidTuneInfo tuneInfo = tune != null ? tune.getInfo() : null;
			if (tuneInfo != null) {
				switch (tuneInfo.getSid2Model()) {
				case MOS6581:
				case MOS8580:
					chipModel = ChipModel
							.valueOf(tuneInfo.getSid2Model().toString());
					break;
				default:
					break;
				}
			}
		}
		return chipModel;
	}
}