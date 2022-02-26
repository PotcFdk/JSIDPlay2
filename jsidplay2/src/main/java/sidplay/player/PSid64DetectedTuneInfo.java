package sidplay.player;

import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;

public final class PSid64DetectedTuneInfo {

	private final boolean detected;
	private final CPUClock cpuClock;
	private final List<ChipModel> chipModels;
	private final int stereoAddress;

	public PSid64DetectedTuneInfo(boolean detected, CPUClock cpuClock, List<ChipModel> chipModels, int stereoAddress) {
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

	public ChipModel getUserChipModel() {
		return chipModels.get(0);
	}

	public ChipModel getStereoChipModel() {
		return chipModels.get(1);
	}

	public int getStereoAddress() {
		return stereoAddress;
	}

	public boolean hasDifferentCPUClock(CPUClock clock) {
		return cpuClock != null && cpuClock != clock;
	}

	public boolean hasDifferentUserChipModel(ChipModel userSidModel) {
		return chipModels.size() > 0 && userSidModel != chipModels.get(0);
	}

	public boolean hasDifferentStereoChipModel(ChipModel stereoSidModel) {
		return chipModels.size() > 1 && stereoSidModel != chipModels.get(1);
	}

	public boolean hasDifferentStereoAddress(int dualSidBase) {
		return stereoAddress != 0 && dualSidBase != stereoAddress;
	}

}