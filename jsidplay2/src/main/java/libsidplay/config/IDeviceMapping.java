package libsidplay.config;

import libsidplay.common.ChipModel;

public interface IDeviceMapping {

	boolean isUsed();

	void setUsed(boolean used);

	String getSerialNum();

	void setSerialNum(String serialNum);

	ChipModel getChipModel();

	void setChipModel(ChipModel chipModel);
}
