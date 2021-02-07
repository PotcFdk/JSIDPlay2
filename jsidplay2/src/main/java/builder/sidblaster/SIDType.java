package builder.sidblaster;

import static libsidplay.common.ChipModel.AUTO;
import static libsidplay.common.ChipModel.MOS6581;
import static libsidplay.common.ChipModel.MOS8580;

import libsidplay.common.ChipModel;

public enum SIDType {
	SIDTYPE_NONE(AUTO), SIDTYPE_6581(MOS6581), SIDTYPE_8580(MOS8580);

	private ChipModel chipModel;

	private SIDType(final ChipModel chipModel) {
		this.chipModel = chipModel;
	}

	public ChipModel asChipModel() {
		return chipModel;
	}

}
