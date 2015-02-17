package sidplay.ini;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import sidplay.ini.intf.IEmulationSection;

/**
 * Emulation section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniEmulationSection extends IniSection implements
		IEmulationSection {
	protected IniEmulationSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public Engine getEngine() {
		return iniReader.getPropertyEnum("Emulation", "Engine", DEFAULT_ENGINE);
	}

	@Override
	public void setEngine(Engine engine) {
		iniReader.setProperty("Emulation", "Engine", engine);
	}

	@Override
	public Emulation getDefaultEmulation() {
		return iniReader.getPropertyEnum("Emulation", "DefaultEmulation",
				DEFAULT_EMULATION);
	}

	@Override
	public void setDefaultEmulation(Emulation emulation) {
		iniReader.setProperty("Emulation", "DefaultEmulation", emulation);
	}

	@Override
	public final Emulation getUserEmulation() {
		return iniReader.getPropertyEnum("Emulation", "UserEmulation", null,
				Emulation.class);
	}

	@Override
	public final void setUserEmulation(final Emulation emulation) {
		iniReader.setProperty("Emulation", "UserEmulation", emulation);
	}

	@Override
	public final Emulation getStereoEmulation() {
		return iniReader.getPropertyEnum("Emulation", "StereoEmulation", null,
				Emulation.class);
	}

	@Override
	public final void setStereoEmulation(final Emulation model) {
		iniReader.setProperty("Emulation", "StereoEmulation", model);
	}

	@Override
	public final Emulation getThirdEmulation() {
		return iniReader.getPropertyEnum("Emulation", "3rdEmulation", null,
				Emulation.class);
	}

	@Override
	public final void setThirdEmulation(final Emulation emulation) {
		iniReader.setProperty("Emulation", "3rdEmulation", emulation);
	}

	@Override
	public final CPUClock getDefaultClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "DefaultClockSpeed",
				DEFAULT_CLOCK_SPEED);
	}

	@Override
	public final void setDefaultClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "DefaultClockSpeed", speed);
	}

	@Override
	public final CPUClock getUserClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "UserClockSpeed", null,
				CPUClock.class);
	}

	@Override
	public final void setUserClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "UserClockSpeed", speed);
	}

	@Override
	public final ChipModel getDefaultSidModel() {
		return iniReader.getPropertyEnum("Emulation", "DefaultSidModel",
				DEFAULT_SID_MODEL);
	}

	@Override
	public final void setDefaultSidModel(ChipModel model) {
		iniReader.setProperty("Emulation", "DefaultSidModel", model);
	}

	@Override
	public final ChipModel getUserSidModel() {
		return iniReader.getPropertyEnum("Emulation", "UserSidModel", null,
				ChipModel.class);
	}

	@Override
	public final void setUserSidModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "UserSidModel", model);
	}

	@Override
	public final ChipModel getStereoSidModel() {
		return iniReader.getPropertyEnum("Emulation", "StereoSidModel", null,
				ChipModel.class);
	}

	@Override
	public final ChipModel getThirdSIDModel() {
		return iniReader.getPropertyEnum("Emulation", "3rdSIDModel", null,
				ChipModel.class);
	}

	@Override
	public final void setStereoSidModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "StereoSidModel", model);
	}

	@Override
	public final void setThirdSIDModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "3rdSIDModel", model);
	}

	@Override
	public final int getHardsid6581() {
		return iniReader.getPropertyInt("Emulation", "HardSID6581",
				DEFAULT_HARD_SID_6581);
	}

	@Override
	public final void setHardsid6581(final int chip) {
		iniReader.setProperty("Emulation", "HardSID6581", chip);
	}

	@Override
	public final int getHardsid8580() {
		return iniReader.getPropertyInt("Emulation", "HardSID8580",
				DEFAULT_HARD_SID_8580);
	}

	@Override
	public final void setHardsid8580(final int chip) {
		iniReader.setProperty("Emulation", "HardSID8580", chip);
	}

	@Override
	public final boolean isFilter() {
		return iniReader.getPropertyBool("Emulation", "UseFilter",
				DEFAULT_USE_FILTER);
	}

	@Override
	public final boolean isStereoFilter() {
		return iniReader.getPropertyBool("Emulation", "UseStereoFilter",
				DEFAULT_USE_STEREO_FILTER);
	}

	@Override
	public final boolean isThirdSIDFilter() {
		return iniReader.getPropertyBool("Emulation", "Use3rdSIDFilter",
				DEFAULT_USE_3SID_FILTER);
	}

	@Override
	public final void setFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseFilter", enable);
	}

	@Override
	public final void setStereoFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseStereoFilter", enable);
	}

	@Override
	public final void setThirdSIDFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "Use3rdSIDFilter", enable);
	}

	@Override
	public int getSidNumToRead() {
		return iniReader.getPropertyInt("Emulation", "SidNumToRead",
				DEFAULT_SID_NUM_TO_READ);
	}

	@Override
	public void setSidNumToRead(int sidNumToRead) {
		iniReader.setProperty("Emulation", "SidNumToRead", sidNumToRead);
	}

	@Override
	public final boolean isDigiBoosted8580() {
		return iniReader.getPropertyBool("Emulation", "DigiBoosted8580",
				DEFAULT_DIGI_BOOSTED_8580);
	}

	@Override
	public final void setDigiBoosted8580(final boolean boost) {
		iniReader.setProperty("Emulation", "DigiBoosted8580", boost);
	}

	@Override
	public final int getDualSidBase() {
		return iniReader.getPropertyInt("Emulation", "dualSidBase",
				DEFAULT_DUAL_SID_BASE);
	}

	@Override
	public final int getThirdSIDBase() {
		return iniReader.getPropertyInt("Emulation", "thirdSIDBase",
				DEFAULT_THIRD_SID_BASE);
	}

	@Override
	public final void setDualSidBase(final int base) {
		iniReader.setProperty("Emulation", "dualSidBase",
				String.format("0x%04x", base));
	}

	@Override
	public final void setThirdSIDBase(final int base) {
		iniReader.setProperty("Emulation", "thirdSIDBase",
				String.format("0x%04x", base));
	}

	@Override
	public final boolean isForceStereoTune() {
		return iniReader.getPropertyBool("Emulation", "forceStereoTune",
				DEFAULT_FORCE_STEREO_TUNE);
	}

	@Override
	public final boolean isForce3SIDTune() {
		return iniReader.getPropertyBool("Emulation", "force3SIDTune",
				DEFAULT_FORCE_3SID_TUNE);
	}

	@Override
	public final void setForceStereoTune(final boolean force) {
		iniReader.setProperty("Emulation", "forceStereoTune", force);
	}

	@Override
	public final void setForce3SIDTune(final boolean force) {
		iniReader.setProperty("Emulation", "force3SIDTune", force);
	}

	@Override
	public final String getFilter6581() {
		return iniReader.getPropertyString("Emulation", "Filter6581",
				DEFAULT_FILTER_6581);
	}

	@Override
	public final String getStereoFilter6581() {
		return iniReader.getPropertyString("Emulation", "Stereo_Filter6581",
				DEFAULT_STEREO_FILTER_6581);
	}

	@Override
	public final String getThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "3rdSID_Filter6581",
				DEFAULT_3SID_FILTER_6581);
	}

	@Override
	public final void setFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Filter6581", filterName);
	}

	@Override
	public final void setStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Stereo_Filter6581", filterName);
	}

	@Override
	public final void setThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "3rdSID_Filter6581", filterName);
	}

	@Override
	public final String getFilter8580() {
		return iniReader.getPropertyString("Emulation", "Filter8580",
				DEFAULT_FILTER_8580);
	}

	@Override
	public final String getStereoFilter8580() {
		return iniReader.getPropertyString("Emulation", "Stereo_Filter8580",
				DEFAULT_STEREO_FILTER_8580);
	}

	@Override
	public final String getThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "3rdSID_Filter8580",
				DEFAULT_3SID_FILTER_8580);
	}

	@Override
	public final void setFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Filter8580", filterName);
	}

	@Override
	public final void setStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Stereo_Filter8580", filterName);
	}

	@Override
	public final void setThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "3rdSID_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter6581",
				DEFAULT_ReSIDfp_FILTER_6581);
	}

	@Override
	public final String getReSIDfpStereoFilter6581() {
		return iniReader
				.getPropertyString("Emulation", "ReSIDfp_Stereo_Filter6581",
						DEFAULT_ReSIDfp_STEREO_FILTER_6581);
	}

	@Override
	public final String getReSIDfpThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation",
				"ReSIDfp_3rdSID_Filter6581", DEFAULT_ReSIDfp_3SID_FILTER_6581);
	}

	@Override
	public final void setReSIDfpFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter6581", filterName);
	}

	@Override
	public final void setReSIDfpStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Stereo_Filter6581",
				filterName);
	}

	@Override
	public final void setReSIDfpThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_3rdSID_Filter6581",
				filterName);
	}

	@Override
	public final String getReSIDfpFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter8580",
				DEFAULT_ReSIDfp_FILTER_8580);
	}

	@Override
	public final String getReSIDfpStereoFilter8580() {
		return iniReader
				.getPropertyString("Emulation", "ReSIDfp_Stereo_Filter8580",
						DEFAULT_ReSIDfp_STEREO_FILTER_8580);
	}

	@Override
	public final String getReSIDfpThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation",
				"ReSIDfp_3rdSID_Filter8580", DEFAULT_ReSIDfp_3SID_FILTER_8580);
	}

	public final void setReSIDfpFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter8580", filterName);
	}

	@Override
	public final void setReSIDfpStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Stereo_Filter8580",
				filterName);
	}

	@Override
	public final void setReSIDfpThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_3rdSID_Filter8580",
				filterName);
	}

}