package sidplay.ini;

import libsidplay.common.ChipModel;
import libsidplay.config.IDeviceMapping;

public class IniDeviceMapping implements IDeviceMapping {

	private String serialNum;
	private ChipModel chipModel;

	public IniDeviceMapping() {
	}

	public IniDeviceMapping(String serialNum, ChipModel chipModel) {
		this.serialNum = serialNum;
		this.chipModel = chipModel;
	}

	@Override
	public String getSerialNum() {
		return serialNum;
	}

	@Override
	public void setSerialNum(String serialNum) {
		this.serialNum = serialNum;
	}

	@Override
	public ChipModel getChipModel() {
		return chipModel;
	}

	@Override
	public void setChipModel(ChipModel chipModel) {
		this.chipModel = chipModel;
	}

}
