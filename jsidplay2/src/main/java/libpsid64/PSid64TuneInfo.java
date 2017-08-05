package libpsid64;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;

public class PSid64TuneInfo {

	protected CPUClock cpuClock;
	protected ChipModel[] chipModels;
	protected int stereoAddress;

	public CPUClock getCpuClock() {
		return cpuClock;
	}

	public boolean hasDifferentCPUClock(CPUClock clock) {
		return cpuClock != null && cpuClock != clock;
	}

	public boolean hasDifferentUserChipModel(ChipModel userSidModel) {
		return chipModels.length > 0 && userSidModel != chipModels[0];
	}

	public ChipModel getUserChipModel() {
		return chipModels[0];
	}

	public boolean hasDifferentStereoChipModel(ChipModel stereoSidModel) {
		return chipModels.length > 1 && stereoSidModel != chipModels[1];
	}

	public ChipModel getStereoChipModel() {
		return chipModels[1];
	}

	public boolean isMonoTune() {
		return chipModels.length == 1;
	}

	public boolean isStereoTune() {
		return chipModels.length == 2;
	}

	public boolean hasDifferentStereoAddress(int dualSidBase) {
		return stereoAddress != 0 && dualSidBase != stereoAddress;
	}

	public int getStereoAddress() {
		return stereoAddress;
	}
}