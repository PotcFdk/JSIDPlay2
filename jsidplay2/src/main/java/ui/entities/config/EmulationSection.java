package ui.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import sidplay.ini.intf.IEmulationSection;

@Embeddable
public class EmulationSection implements IEmulationSection {

	private Emulation emulation = Emulation.RESID;

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getEmulation() {
		return this.emulation;
	}

	@Override
	public void setEmulation(Emulation emulation) {
		this.emulation = emulation;
	}

	private CPUClock defaultClockSpeed = CPUClock.PAL;

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

	private ChipModel defaultSidModel = ChipModel.MOS6581;

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

	private int hardsid6581 = 1;

	@Override
	public int getHardsid6581() {
		return hardsid6581;
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581 = hardsid6581;
	}

	private int hardsid8580 = 2;

	@Override
	public int getHardsid8580() {
		return hardsid8580;
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580 = hardsid8580;
	}

	private boolean filter = true;

	@Override
	public boolean isFilter() {
		return filter;
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter = isFilter;
	}

	private String filter6581 = "FilterAverage6581";

	@Override
	public String getFilter6581() {
		return filter6581;
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581 = filter6581;
	}

	private String stereoFilter6581 = "FilterAverage6581";

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581;
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581 = filter6581;
	}

	private String filter8580 = "FilterAverage8580";

	@Override
	public String getFilter8580() {
		return filter8580;
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580 = filter8580;
	}

	private String stereoFilter8580 = "FilterAverage8580";

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580;
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580 = filter8580;
	}

	private String reSIDfpFilter6581 = "FilterAlankila6581R4AR_3789";

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581;
	}

	@Override
	public void setReSIDfpFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfpStereoFilter6581 = "FilterAlankila6581R4AR_3789";

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581;
	}

	@Override
	public void setReSIDfpStereoFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpStereoFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfpFilter8580 = "FilterTrurl8580R5_3691";

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580;
	}

	@Override
	public void setReSIDfpFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpFilter8580 = reSIDfpFilter8580;
	}

	private String reSIDfpStereoFilter8580 = "FilterTrurl8580R5_3691";

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580;
	}

	@Override
	public void setReSIDfpStereoFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpStereoFilter8580 = reSIDfpFilter8580;
	}

	private boolean digiBoosted8580;

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580;
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580 = isDigiBoosted8580;
	}

	private int dualSidBase = 0xd420;

	@Override
	public int getDualSidBase() {
		return dualSidBase;
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase = dualSidBase;
	}

	private boolean forceStereoTune;

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune;
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune = isForceStereoTune;
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
}
