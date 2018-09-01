package libsidutils.psid64;

import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;

public class PSid64TuneInfo {

	private final boolean detected;
	private final CPUClock cpuClock;
	private final List<ChipModel> chipModels;
	private final int stereoAddress;

	public PSid64TuneInfo(boolean detected, CPUClock cpuClock, List<ChipModel> chipModels, int stereoAddress) {
		this.detected = detected;
		this.cpuClock = cpuClock;
		this.chipModels = chipModels;
		this.stereoAddress = stereoAddress;
	}

	public boolean isDetected() {
		return detected;
	}

	public CPUClock getCpuClock() {
		return cpuClock;
	}

	public boolean hasDifferentCPUClock(CPUClock clock) {
		return cpuClock != null && cpuClock != clock;
	}

	public boolean hasDifferentUserChipModel(ChipModel userSidModel) {
		return chipModels.size() > 0 && userSidModel != chipModels.get(0);
	}

	public ChipModel getUserChipModel() {
		return chipModels.get(0);
	}

	public boolean hasDifferentStereoChipModel(ChipModel stereoSidModel) {
		return chipModels.size() > 1 && stereoSidModel != chipModels.get(1);
	}

	public ChipModel getStereoChipModel() {
		return chipModels.get(1);
	}

	public boolean isMonoTune() {
		return chipModels.size() == 1;
	}

	public boolean isStereoTune() {
		return chipModels.size() == 2;
	}

	public boolean hasDifferentStereoAddress(int dualSidBase) {
		return stereoAddress != 0 && dualSidBase != stereoAddress;
	}

	public int getStereoAddress() {
		return stereoAddress;
	}

}