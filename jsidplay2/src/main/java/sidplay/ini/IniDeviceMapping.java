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
	public final boolean isUsed() {
		return used;
	}

	@Override
	public final void setUsed(boolean used) {
		this.used = used;
	}

	@Override
	public final String getSerialNum() {
		return serialNum;
	}

	@Override
	public final void setSerialNum(String serialNum) {
		this.serialNum = serialNum;
	}

	@Override
	public final ChipModel getChipModel() {
		return chipModel;
	}

	@Override
	public final void setChipModel(ChipModel chipModel) {
		this.chipModel = chipModel;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
