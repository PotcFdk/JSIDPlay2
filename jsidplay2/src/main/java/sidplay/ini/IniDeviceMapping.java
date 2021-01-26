package sidplay.ini;

import libsidplay.common.ChipModel;
import libsidplay.config.IDeviceMapping;
import sidplay.ini.converter.BeanToStringConverter;

public class IniDeviceMapping implements IDeviceMapping {

	private boolean used;
	private String serialNum;
	private ChipModel chipModel;

	public IniDeviceMapping() {
	}

	public IniDeviceMapping(String serialNum, ChipModel chipModel, boolean used) {
		this.serialNum = serialNum;
		this.chipModel = chipModel;
		this.used = used;
	}

	@Override
	public boolean isUsed() {
		return used;
	}

	@Override
	public void setUsed(boolean used) {
		this.used = used;
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

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
