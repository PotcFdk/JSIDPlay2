package applet.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.common.ISID2Types.CPUClock;
import resid_builder.resid.ISIDDefs.ChipModel;
import sidplay.ini.intf.IEmulationSection;
import applet.config.annotations.ConfigDescription;

@Embeddable
public class EmulationSection implements IEmulationSection {

	@Enumerated(EnumType.STRING)
	@ConfigDescription(bundleKey = "EMULATION_DEFAULT_CLOCK_SPEED_DESC", toolTipBundleKey = "EMULATION_DEFAULT_CLOCK_SPEED_TOOLTIP")
	private CPUClock defaultClockSpeed = CPUClock.PAL;

	@Override
	public CPUClock getDefaultClockSpeed() {
		return this.defaultClockSpeed;
	}

	@Override
	public void setDefaultClockSpeed(CPUClock speed) {
		this.defaultClockSpeed = speed;
	}

	@Enumerated(EnumType.STRING)
	@ConfigDescription(bundleKey = "EMULATION_USER_CLOCK_SPEED_DESC", toolTipBundleKey = "EMULATION_USER_CLOCK_SPEED_TOOLTIP")
	private CPUClock userClockSpeed;

	@Override
	public CPUClock getUserClockSpeed() {
		return userClockSpeed;
	}

	@Override
	public void setUserClockSpeed(CPUClock userClockSpeed) {
		this.userClockSpeed = userClockSpeed;
	}

	@Enumerated(EnumType.STRING)
	@ConfigDescription(bundleKey = "EMULATION_DEFAULT_SID_MODEL_DESC", toolTipBundleKey = "EMULATION_DEFAULT_SID_MODEL_TOOLTIP")
	private ChipModel defaultSidModel = ChipModel.MOS6581;

	@Override
	public ChipModel getDefaultSidModel() {
		return defaultSidModel;
	}

	@Override
	public void setDefaultSidModel(ChipModel defaultSidModel) {
		this.defaultSidModel = defaultSidModel;
	}

	@Enumerated(EnumType.STRING)
	@ConfigDescription(bundleKey = "EMULATION_USER_SID_MODEL_DESC", toolTipBundleKey = "EMULATION_USER_SID_MODEL_TOOLTIP")
	private ChipModel userSidModel;

	@Override
	public ChipModel getUserSidModel() {
		return userSidModel;
	}

	@Override
	public void setUserSidModel(ChipModel userSidModel) {
		this.userSidModel = userSidModel;
	}

	@ConfigDescription(bundleKey = "EMULATION_HARDSID6581_DESC", toolTipBundleKey = "EMULATION_HARDSID6581_TOOLTIP")
	private int hardsid6581 = 1;

	@Override
	public int getHardsid6581() {
		return hardsid6581;
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581 = hardsid6581;
	}

	@ConfigDescription(bundleKey = "EMULATION_HARDSID8580_DESC", toolTipBundleKey = "EMULATION_HARDSID8580_TOOLTIP")
	private int hardsid8580 = 2;

	@Override
	public int getHardsid8580() {
		return hardsid8580;
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580 = hardsid8580;
	}

	@ConfigDescription(bundleKey = "EMULATION_FILTER_DESC", toolTipBundleKey = "EMULATION_FILTER_TOOLTIP")
	private boolean filter = true;

	@Override
	public boolean isFilter() {
		return filter;
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter = isFilter;
	}

	@ConfigDescription(bundleKey = "EMULATION_FILTER6581_DESC", toolTipBundleKey = "EMULATION_FILTER6581_TOOLTIP")
	private String filter6581 = "FilterAverage6581";

	@Override
	public String getFilter6581() {
		return filter6581;
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581 = filter6581;
	}

	@ConfigDescription(bundleKey = "EMULATION_FILTER8580_DESC", toolTipBundleKey = "EMULATION_FILTER8580_TOOLTIP")
	private String filter8580 = "FilterAverage8580";

	@Override
	public String getFilter8580() {
		return filter8580;
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580 = filter8580;
	}

	@ConfigDescription(bundleKey = "EMULATION_DIGI_BOOSTED8580_DESC", toolTipBundleKey = "EMULATION_DIGI_BOOSTED8580_TOOLTIP")
	private boolean digiBoosted8580;

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580;
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580 = isDigiBoosted8580;
	}

	@ConfigDescription(bundleKey = "EMULATION_DUAL_SID_BASE_DESC", toolTipBundleKey = "EMULATION_DUAL_SID_BASE_TOOLTIP")
	private int dualSidBase = 0xd420;

	@Override
	public int getDualSidBase() {
		return dualSidBase;
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase = dualSidBase;
	}

	@ConfigDescription(bundleKey = "EMULATION_FORCE_STEREO_TUNE_DESC", toolTipBundleKey = "EMULATION_FORCE_STEREO_TUNE_TOOLTIP")
	private boolean forceStereoTune;

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune;
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune = isForceStereoTune;
	}

	@Enumerated(EnumType.STRING)
	@ConfigDescription(bundleKey = "EMULATION_STEREO_SID_MODEL_DESC", toolTipBundleKey = "EMULATION_STEREO_SID_MODEL_TOOLTIP")
	private ChipModel stereoSidModel;

	@Override
	public ChipModel getStereoSidModel() {
		return stereoSidModel;
	}

	@Override
	public void setStereoSidModel(ChipModel stereoSidModel) {
		this.stereoSidModel = stereoSidModel;
	}
}
