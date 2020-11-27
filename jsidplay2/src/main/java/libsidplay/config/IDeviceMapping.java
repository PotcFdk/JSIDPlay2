package libsidplay.config;

import libsidplay.common.ChipModel;

public interface IDeviceMapping {

	String getSerialNum();

	void setSerialNum(String serialNum);

	ChipModel getChipModel();

	void setChipModel(ChipModel chipModel);
}
