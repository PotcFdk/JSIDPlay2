package ui.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import sidplay.ini.intf.IEmulationSection;

@Embeddable
public class EmulationSection implements IEmulationSection {

	private Engine engine = DEFAULT_ENGINE;

	@Enumerated(EnumType.STRING)
	@Override
	public Engine getEngine() {
		return this.engine;
	}

	@Override
	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	private Emulation defaultEmulation = DEFAULT_EMULATION;

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getDefaultEmulation() {
		return this.defaultEmulation;
	}

	@Override
	public void setDefaultEmulation(Emulation emulation) {
		this.defaultEmulation = emulation;
	}

	private Emulation userEmulation;

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getUserEmulation() {
		return this.userEmulation;
	}

	@Override
	public void setUserEmulation(Emulation userEmulation) {
		this.userEmulation = userEmulation;
	}

	private Emulation stereoEmulation;

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getStereoEmulation() {
		return this.stereoEmulation;
	}

	@Override
	public void setStereoEmulation(Emulation stereoEmulation) {
		this.stereoEmulation = stereoEmulation;
	}

	private Emulation thirdEmulation;

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getThirdEmulation() {
		return this.thirdEmulation;
	}

	@Override
	public void setThirdEmulation(Emulation thirdEmulation) {
		this.thirdEmulation = thirdEmulation;
	}
	private CPUClock defaultClockSpeed = DEFAULT_CLOCK_SPEED;

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getDefaultClockSpeed() {
		return this.defaultClockSpeed;
	}

	@Override
	public void setDefaultClockSpeed(CPUClock speed) {
		this.defaultClockSpeed = speed;
	}

	private CPUClock userClockSpeed;

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getUserClockSpeed() {
		return userClockSpeed;
	}

	@Override
	public void setUserClockSpeed(CPUClock userClockSpeed) {
		this.userClockSpeed = userClockSpeed;
	}

	private ChipModel defaultSidModel = DEFAULT_SID_MODEL;

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getDefaultSidModel() {
		return defaultSidModel;
	}

	@Override
	public void setDefaultSidModel(ChipModel defaultSidModel) {
		this.defaultSidModel = defaultSidModel;
	}

	private ChipModel userSidModel;

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getUserSidModel() {
		return userSidModel;
	}

	@Override
	public void setUserSidModel(ChipModel userSidModel) {
		this.userSidModel = userSidModel;
	}

	private int hardsid6581 = DEFAULT_HARD_SID_6581;

	@Override
	public int getHardsid6581() {
		return hardsid6581;
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581 = hardsid6581;
	}

	private int hardsid8580 = DEFAULT_HARD_SID_8580;

	@Override
	public int getHardsid8580() {
		return hardsid8580;
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580 = hardsid8580;
	}

	private boolean filter = DEFAULT_USE_FILTER;

	@Override
	public boolean isFilter() {
		return filter;
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter = isFilter;
	}

	private boolean stereoFilter = DEFAULT_USE_STEREO_FILTER;

	@Override
	public boolean isStereoFilter() {
		return stereoFilter;
	}

	@Override
	public void setStereoFilter(boolean isFilter) {
		this.stereoFilter = isFilter;
	}

	private boolean thirdSIDFilter = DEFAULT_USE_3SID_FILTER;

	@Override
	public boolean isThirdSIDFilter() {
		return thirdSIDFilter;
	}

	@Override
	public void setThirdSIDFilter(boolean isFilter) {
		this.thirdSIDFilter = isFilter;
	}

	private int sidNumToRead = DEFAULT_SID_NUM_TO_READ;

	@Override
	public int getSidNumToRead() {
		return sidNumToRead;
	}

	@Override
	public void setSidNumToRead(int sidNumToRead) {
		this.sidNumToRead = sidNumToRead;
	}

	private boolean digiBoosted8580 = DEFAULT_DIGI_BOOSTED_8580;

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580;
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580 = isDigiBoosted8580;
	}

	private int dualSidBase = DEFAULT_DUAL_SID_BASE;

	@Override
	public int getDualSidBase() {
		return dualSidBase;
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase = dualSidBase;
	}

	private int thirdSIDBase = DEFAULT_THIRD_SID_BASE;

	@Override
	public int getThirdSIDBase() {
		return thirdSIDBase;
	}

	@Override
	public void setThirdSIDBase(int dualSidBase) {
		this.thirdSIDBase = dualSidBase;
	}

	private boolean forceStereoTune = DEFAULT_FORCE_STEREO_TUNE;

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune;
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune = isForceStereoTune;
	}

	private boolean force3SIDTune = DEFAULT_FORCE_3SID_TUNE;

	@Override
	public boolean isForce3SIDTune() {
		return force3SIDTune;
	}

	@Override
	public void setForce3SIDTune(boolean isForceStereoTune) {
		this.force3SIDTune = isForceStereoTune;
	}

	private ChipModel stereoSidModel;

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getStereoSidModel() {
		return stereoSidModel;
	}

	@Override
	public void setStereoSidModel(ChipModel stereoSidModel) {
		this.stereoSidModel = stereoSidModel;
	}

	private ChipModel thirdSIDModel;

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getThirdSIDModel() {
		return thirdSIDModel;
	}

	@Override
	public void setThirdSIDModel(ChipModel stereoSidModel) {
		this.thirdSIDModel = stereoSidModel;
	}

	private String filter6581 = DEFAULT_FILTER_6581;

	@Override
	public String getFilter6581() {
		return filter6581;
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581 = filter6581;
	}

	private String stereoFilter6581 = DEFAULT_STEREO_FILTER_6581;

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581;
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581 = filter6581;
	}

	private String thirdSIDFilter6581 = DEFAULT_3SID_FILTER_6581;

	@Override
	public String getThirdSIDFilter6581() {
		return thirdSIDFilter6581;
	}

	@Override
	public void setThirdSIDFilter6581(String filter6581) {
		this.thirdSIDFilter6581 = filter6581;
	}

	private String filter8580 = DEFAULT_FILTER_8580;

	@Override
	public String getFilter8580() {
		return filter8580;
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580 = filter8580;
	}

	private String stereoFilter8580 = DEFAULT_STEREO_FILTER_8580;

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580;
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580 = filter8580;
	}

	private String thirdSIDFilter8580 = DEFAULT_3SID_FILTER_8580;

	@Override
	public String getThirdSIDFilter8580() {
		return thirdSIDFilter8580;
	}

	@Override
	public void setThirdSIDFilter8580(String filter8580) {
		this.thirdSIDFilter8580 = filter8580;
	}

	private String reSIDfpFilter6581 = DEFAULT_ReSIDfp_FILTER_6581;

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581;
	}

	@Override
	public void setReSIDfpFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfpStereoFilter6581 = DEFAULT_ReSIDfp_STEREO_FILTER_6581;

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581;
	}

	@Override
	public void setReSIDfpStereoFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpStereoFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfp3rdSIDFilter6581 = DEFAULT_ReSIDfp_3SID_FILTER_6581;

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return reSIDfp3rdSIDFilter6581;
	}

	@Override
	public void setReSIDfpThirdSIDFilter6581(String reSIDfpFilter6581) {
		this.reSIDfp3rdSIDFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfpFilter8580 = DEFAULT_ReSIDfp_FILTER_8580;

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580;
	}

	@Override
	public void setReSIDfpFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpFilter8580 = reSIDfpFilter8580;
	}

	private String reSIDfpStereoFilter8580 = DEFAULT_ReSIDfp_STEREO_FILTER_8580;

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580;
	}

	@Override
	public void setReSIDfpStereoFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpStereoFilter8580 = reSIDfpFilter8580;
	}

	private String reSIDfp3rdSIDFilter8580 = DEFAULT_ReSIDfp_3SID_FILTER_8580;

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return reSIDfp3rdSIDFilter8580;
	}

	@Override
	public void setReSIDfpThirdSIDFilter8580(String reSIDfpFilter8580) {
		this.reSIDfp3rdSIDFilter8580 = reSIDfpFilter8580;
	}

}
