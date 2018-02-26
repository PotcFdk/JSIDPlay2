package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_3SID_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_DIGI_BOOSTED_8580;
import static sidplay.ini.IniDefaults.DEFAULT_DUAL_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_ENGINE;
import static sidplay.ini.IniDefaults.DEFAULT_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_3SID_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_STEREO_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_6581;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_8580;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_NETSIDDEV_HOST;
import static sidplay.ini.IniDefaults.DEFAULT_NETSIDDEV_PORT;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SID_NUM_TO_READ;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_USER_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_USER_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_USER_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.config.IEmulationSection;

/**
 * Emulation section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
@Parameters(resourceBundle = "sidplay.ini.IniEmulationSection")
public class IniEmulationSection extends IniSection implements IEmulationSection {
	protected IniEmulationSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public Engine getEngine() {
		return iniReader.getPropertyEnum("Emulation", "Engine", DEFAULT_ENGINE, Engine.class);
	}

	@Override
	@Parameter(names = { "--engine", "-E" }, descriptionKey = "ENGINE")
	public void setEngine(Engine engine) {
		iniReader.setProperty("Emulation", "Engine", engine);
	}

	@Override
	public Emulation getDefaultEmulation() {
		return iniReader.getPropertyEnum("Emulation", "DefaultEmulation", DEFAULT_EMULATION, Emulation.class);
	}

	@Override
	@Parameter(names = { "--defaultEmulation", "-e" }, descriptionKey = "DEFAULT_EMULATION")
	public void setDefaultEmulation(Emulation emulation) {
		iniReader.setProperty("Emulation", "DefaultEmulation", emulation);
	}

	@Override
	public final Emulation getUserEmulation() {
		return iniReader.getPropertyEnum("Emulation", "UserEmulation", DEFAULT_USER_EMULATION, Emulation.class);
	}

	@Override
	public final void setUserEmulation(final Emulation emulation) {
		iniReader.setProperty("Emulation", "UserEmulation", emulation);
	}

	@Override
	public final Emulation getStereoEmulation() {
		return iniReader.getPropertyEnum("Emulation", "StereoEmulation", DEFAULT_STEREO_EMULATION, Emulation.class);
	}

	@Override
	public final void setStereoEmulation(final Emulation model) {
		iniReader.setProperty("Emulation", "StereoEmulation", model);
	}

	@Override
	public final Emulation getThirdEmulation() {
		return iniReader.getPropertyEnum("Emulation", "3rdEmulation", DEFAULT_3SID_EMULATION, Emulation.class);
	}

	@Override
	public final void setThirdEmulation(final Emulation emulation) {
		iniReader.setProperty("Emulation", "3rdEmulation", emulation);
	}

	@Override
	public final CPUClock getDefaultClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "DefaultClockSpeed", DEFAULT_CLOCK_SPEED, CPUClock.class);
	}

	@Override
	@Parameter(names = { "--defaultClock", "-k" }, descriptionKey = "DEFAULT_CLOCK")
	public final void setDefaultClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "DefaultClockSpeed", speed);
	}

	@Override
	public final CPUClock getUserClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "UserClockSpeed", DEFAULT_USER_CLOCK_SPEED, CPUClock.class);
	}

	@Override
	@Parameter(names = { "--forceClock", "-c" }, descriptionKey = "FORCE_CLOCK")
	public final void setUserClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "UserClockSpeed", speed);
	}

	@Override
	public final ChipModel getDefaultSidModel() {
		return iniReader.getPropertyEnum("Emulation", "DefaultSidModel", DEFAULT_SID_MODEL, ChipModel.class);
	}

	@Override
	@Parameter(names = { "--defaultModel", "-u" }, descriptionKey = "DEFAULT_MODEL")
	public final void setDefaultSidModel(ChipModel model) {
		iniReader.setProperty("Emulation", "DefaultSidModel", model);
	}

	@Override
	public final ChipModel getUserSidModel() {
		return iniReader.getPropertyEnum("Emulation", "UserSidModel", DEFAULT_USER_MODEL, ChipModel.class);
	}

	@Override
	@Parameter(names = { "--forceModel", "-m" }, descriptionKey = "FORCE_MODEL")
	public final void setUserSidModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "UserSidModel", model);
	}

	@Override
	public final ChipModel getStereoSidModel() {
		return iniReader.getPropertyEnum("Emulation", "StereoSidModel", DEFAULT_STEREO_MODEL, ChipModel.class);
	}

	@Override
	public final void setStereoSidModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "StereoSidModel", model);
	}

	@Override
	public final ChipModel getThirdSIDModel() {
		return iniReader.getPropertyEnum("Emulation", "3rdSIDModel", DEFAULT_3SID_MODEL, ChipModel.class);
	}

	@Override
	public final void setThirdSIDModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "3rdSIDModel", model);
	}

	@Override
	public final int getHardsid6581() {
		return iniReader.getPropertyInt("Emulation", "HardSID6581", DEFAULT_HARD_SID_6581);
	}

	@Override
	public final void setHardsid6581(final int chip) {
		iniReader.setProperty("Emulation", "HardSID6581", chip);
	}

	@Override
	public final int getHardsid8580() {
		return iniReader.getPropertyInt("Emulation", "HardSID8580", DEFAULT_HARD_SID_8580);
	}

	@Override
	public final void setHardsid8580(final int chip) {
		iniReader.setProperty("Emulation", "HardSID8580", chip);
	}

	@Override
	public String getNetSIDDevHost() {
		return iniReader.getPropertyString("Emulation", "NetSIDDev Host", DEFAULT_NETSIDDEV_HOST);
	}
	
	@Override
	public void setNetSIDDevHost(String hostname) {
		iniReader.setProperty("Emulation", "NetSIDDev Host", hostname);
	}
	
	@Override
	public int getNetSIDDevPort() {
		return iniReader.getPropertyInt("Emulation", "NetSIDDev Port", DEFAULT_NETSIDDEV_PORT);
	}
	
	@Override
	public void setNetSIDDevPort(int port) {
		iniReader.setProperty("Emulation", "NetSIDDev Port", port);
	}
	
	@Override
	public final boolean isFilter() {
		return iniReader.getPropertyBool("Emulation", "UseFilter", DEFAULT_USE_FILTER);
	}

	@Override
	@Parameter(names = { "--enableFilter", "-i" }, descriptionKey = "ENABLE_FILTER", arity=1)
	public final void setFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseFilter", enable);
	}

	protected final boolean getFilter() {
		return isFilter();
	}
	
	@Override
	public final boolean isStereoFilter() {
		return iniReader.getPropertyBool("Emulation", "UseStereoFilter", DEFAULT_USE_STEREO_FILTER);
	}

	@Override
	@Parameter(names = { "--enableStereoFilter", "-j" }, descriptionKey = "ENABLE_STEREO_FILTER", arity=1)
	public final void setStereoFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseStereoFilter", enable);
	}

	public final boolean getStereoFilter() {
		return isStereoFilter();
	}
	
	@Override
	public final boolean isThirdSIDFilter() {
		return iniReader.getPropertyBool("Emulation", "Use3rdSIDFilter", DEFAULT_USE_3SID_FILTER);
	}

	@Override
	@Parameter(names = { "--enable3rdSidFilter", "-J" }, descriptionKey = "ENABLE_3RD_SID_FILTER", arity=1)
	public final void setThirdSIDFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "Use3rdSIDFilter", enable);
	}

	public final boolean getThirdSIDFilter() {
		return isThirdSIDFilter();
	}
	
	@Override
	public int getSidNumToRead() {
		return iniReader.getPropertyInt("Emulation", "SidNumToRead", DEFAULT_SID_NUM_TO_READ);
	}

	@Override
	public void setSidNumToRead(int sidNumToRead) {
		iniReader.setProperty("Emulation", "SidNumToRead", sidNumToRead);
	}

	@Override
	public final boolean isDigiBoosted8580() {
		return iniReader.getPropertyBool("Emulation", "DigiBoosted8580", DEFAULT_DIGI_BOOSTED_8580);
	}

	@Override
	public final void setDigiBoosted8580(final boolean boost) {
		iniReader.setProperty("Emulation", "DigiBoosted8580", boost);
	}

	@Override
	public final int getDualSidBase() {
		return iniReader.getPropertyInt("Emulation", "dualSidBase", DEFAULT_DUAL_SID_BASE);
	}

	@Override
	public final void setDualSidBase(final int base) {
		iniReader.setProperty("Emulation", "dualSidBase", String.format("0x%04x", base));
	}

	@Override
	public final int getThirdSIDBase() {
		return iniReader.getPropertyInt("Emulation", "thirdSIDBase", DEFAULT_THIRD_SID_BASE);
	}

	@Override
	public final void setThirdSIDBase(final int base) {
		iniReader.setProperty("Emulation", "thirdSIDBase", String.format("0x%04x", base));
	}

	@Override
	public final boolean isFakeStereo() {
		return iniReader.getPropertyBool("Emulation", "fakeStereo", DEFAULT_FAKE_STEREO);
	}

	@Override
	public final void setFakeStereo(boolean fakeStereo) {
		iniReader.setProperty("Emulation", "fakeStereo", fakeStereo);
	}

	@Override
	public final boolean isForceStereoTune() {
		return iniReader.getPropertyBool("Emulation", "forceStereoTune", DEFAULT_FORCE_STEREO_TUNE);
	}

	@Override
	@Parameter(names = { "--dualSID", "-d" }, descriptionKey = "DUAL_SID", arity = 1)
	public final void setForceStereoTune(final boolean force) {
		iniReader.setProperty("Emulation", "forceStereoTune", force);
	}

	public final boolean getForceStereoTune() {
		return isForceStereoTune();
	}
	
	@Override
	public final boolean isForce3SIDTune() {
		return iniReader.getPropertyBool("Emulation", "force3SIDTune", DEFAULT_FORCE_3SID_TUNE);
	}

	@Override
	@Parameter(names = { "--thirdSID", "-D" }, descriptionKey = "THIRD_SID", arity = 1)
	public final void setForce3SIDTune(final boolean force) {
		iniReader.setProperty("Emulation", "force3SIDTune", force);
	}

	public final boolean getForce3SIDTune() {
		return isForce3SIDTune();
	}
	
	@Override
	public boolean isMuteVoice1() {
		return iniReader.getPropertyBool("Emulation", "muteVoice1", DEFAULT_MUTE_VOICE1);
	}

	@Override
	@Parameter(names = { "--muteVoice1", "-1" }, descriptionKey = "MUTE_VOICE_1", arity = 1)
	public void setMuteVoice1(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice1", mute);
	}

	public boolean getMuteVoice1() {
		return isMuteVoice1();
	}
	
	@Override
	public boolean isMuteVoice2() {
		return iniReader.getPropertyBool("Emulation", "muteVoice2", DEFAULT_MUTE_VOICE2);
	}

	@Override
	@Parameter(names = { "--muteVoice2", "-2" }, descriptionKey = "MUTE_VOICE_2", arity = 1)
	public void setMuteVoice2(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice2", mute);
	}

	public boolean getMuteVoice2() {
		return isMuteVoice2();
	}
	
	@Override
	public boolean isMuteVoice3() {
		return iniReader.getPropertyBool("Emulation", "muteVoice3", DEFAULT_MUTE_VOICE3);
	}

	@Override
	@Parameter(names = { "--muteVoice3", "-3" }, descriptionKey = "MUTE_VOICE_3", arity = 1)
	public void setMuteVoice3(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice3", mute);
	}

	public boolean getMuteVoice3() {
		return isMuteVoice3();
	}
	
	@Override
	public boolean isMuteVoice4() {
		return iniReader.getPropertyBool("Emulation", "muteVoice4", DEFAULT_MUTE_VOICE4);
	}

	@Override
	@Parameter(names = { "--muteVoice4", "-4" }, descriptionKey = "MUTE_VOICE_4", arity = 1)
	public void setMuteVoice4(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice4", mute);
	}

	public boolean getMuteVoice4() {
		return isMuteVoice4();
	}
	
	@Override
	public boolean isMuteStereoVoice1() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice1", DEFAULT_MUTE_STEREO_VOICE1);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice1", "-5" }, descriptionKey = "MUTE_VOICE_5", arity = 1)
	public void setMuteStereoVoice1(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice1", mute);
	}

	public boolean getMuteStereoVoice1() {
		return isMuteStereoVoice1();
	}
	
	@Override
	public boolean isMuteStereoVoice2() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice2", DEFAULT_MUTE_STEREO_VOICE2);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice2", "-6" }, descriptionKey = "MUTE_VOICE_6", arity = 1)
	public void setMuteStereoVoice2(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice2", mute);
	}

	public boolean getMuteStereoVoice2() {
		return isMuteStereoVoice2();
	}
	
	@Override
	public boolean isMuteStereoVoice3() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice3", DEFAULT_MUTE_STEREO_VOICE3);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice3", "-7" }, descriptionKey = "MUTE_VOICE_7", arity = 1)
	public void setMuteStereoVoice3(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice3", mute);
	}

	public boolean getMuteStereoVoice3() {
		return isMuteStereoVoice3();
	}
	
	@Override
	public boolean isMuteStereoVoice4() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice4", DEFAULT_MUTE_STEREO_VOICE4);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice4", "-8" }, descriptionKey = "MUTE_VOICE_8", arity = 1)
	public void setMuteStereoVoice4(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice4", mute);
	}

	public boolean getMuteStereoVoice4() {
		return isMuteStereoVoice4();
	}
	
	@Override
	public boolean isMuteThirdSIDVoice1() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice1", DEFAULT_MUTE_THIRDSID_VOICE1);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice1", "-9" }, descriptionKey = "MUTE_VOICE_9", arity = 1)
	public void setMuteThirdSIDVoice1(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice1", mute);
	}

	public boolean getMuteThirdSIDVoice1() {
		return isMuteThirdSIDVoice1();
	}
	
	@Override
	public boolean isMuteThirdSIDVoice2() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice2", DEFAULT_MUTE_THIRDSID_VOICE2);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice2", "-10" }, descriptionKey = "MUTE_VOICE_10", arity = 1)
	public void setMuteThirdSIDVoice2(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice2", mute);
	}

	public boolean getMuteThirdSIDVoice2() {
		return isMuteThirdSIDVoice2();
	}
	
	@Override
	public boolean isMuteThirdSIDVoice3() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice3", DEFAULT_MUTE_THIRDSID_VOICE3);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice3", "-11" }, descriptionKey = "MUTE_VOICE_11", arity = 1)
	public void setMuteThirdSIDVoice3(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice3", mute);
	}

	public boolean getMuteThirdSIDVoice3() {
		return isMuteThirdSIDVoice3();
	}
	
	@Override
	public boolean isMuteThirdSIDVoice4() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice4", DEFAULT_MUTE_THIRDSID_VOICE4);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice4", "-12" }, descriptionKey = "MUTE_VOICE_12", arity = 1)
	public void setMuteThirdSIDVoice4(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice4", mute);
	}

	public boolean getMuteThirdSIDVoice4() {
		return isMuteThirdSIDVoice4();
	}
	
	@Override
	public final String getNetSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "NetSID_Filter6581", DEFAULT_NETSID_FILTER_6581);
	}

	@Override
	public final void setNetSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "NetSID_Filter6581", filterName);
	}

	@Override
	public final String getNetSIDStereoFilter6581() {
		return iniReader.getPropertyString("Emulation", "NetSID_Stereo_Filter6581", DEFAULT_NETSID_STEREO_FILTER_6581);
	}

	@Override
	public final void setNetSIDStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "NetSID_Stereo_Filter6581", filterName);
	}

	@Override
	public final String getNetSIDThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "NetSID_3rdSID_Filter6581", DEFAULT_NETSID_3SID_FILTER_6581);
	}

	@Override
	public final void setNetSIDThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "NetSID_3rdSID_Filter6581", filterName);
	}

	@Override
	public final String getNetSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "NetSID_Filter8580", DEFAULT_NETSID_FILTER_8580);
	}

	public final void setNetSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "NetSID_Filter8580", filterName);
	}

	@Override
	public final String getNetSIDStereoFilter8580() {
		return iniReader.getPropertyString("Emulation", "NetSID_Stereo_Filter8580", DEFAULT_NETSID_STEREO_FILTER_8580);
	}

	@Override
	public final void setNetSIDStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "NetSID_Stereo_Filter8580", filterName);
	}

	@Override
	public final String getNetSIDThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "NetSID_3rdSID_Filter8580", DEFAULT_NETSID_3SID_FILTER_8580);
	}

	@Override
	public final void setNetSIDThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "NetSID_3rdSID_Filter8580", filterName);
	}

	@Override
	public final String getFilter6581() {
		return iniReader.getPropertyString("Emulation", "Filter6581", DEFAULT_FILTER_6581);
	}

	@Override
	public final void setFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Filter6581", filterName);
	}

	@Override
	public final String getStereoFilter6581() {
		return iniReader.getPropertyString("Emulation", "Stereo_Filter6581", DEFAULT_STEREO_FILTER_6581);
	}

	@Override
	public final void setStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Stereo_Filter6581", filterName);
	}

	@Override
	public final String getThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "3rdSID_Filter6581", DEFAULT_3SID_FILTER_6581);
	}

	@Override
	public final void setThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "3rdSID_Filter6581", filterName);
	}

	@Override
	public final String getFilter8580() {
		return iniReader.getPropertyString("Emulation", "Filter8580", DEFAULT_FILTER_8580);
	}

	@Override
	public final void setFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Filter8580", filterName);
	}

	@Override
	public final String getStereoFilter8580() {
		return iniReader.getPropertyString("Emulation", "Stereo_Filter8580", DEFAULT_STEREO_FILTER_8580);
	}

	@Override
	public final void setStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Stereo_Filter8580", filterName);
	}

	@Override
	public final String getThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "3rdSID_Filter8580", DEFAULT_3SID_FILTER_8580);
	}

	@Override
	public final void setThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "3rdSID_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter6581", DEFAULT_ReSIDfp_FILTER_6581);
	}

	@Override
	public final void setReSIDfpFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter6581", filterName);
	}

	@Override
	public final String getReSIDfpStereoFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Stereo_Filter6581",
				DEFAULT_ReSIDfp_STEREO_FILTER_6581);
	}

	@Override
	public final void setReSIDfpStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Stereo_Filter6581", filterName);
	}

	@Override
	public final String getReSIDfpThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_3rdSID_Filter6581", DEFAULT_ReSIDfp_3SID_FILTER_6581);
	}

	@Override
	public final void setReSIDfpThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_3rdSID_Filter6581", filterName);
	}

	@Override
	public final String getReSIDfpFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter8580", DEFAULT_ReSIDfp_FILTER_8580);
	}

	public final void setReSIDfpFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpStereoFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Stereo_Filter8580",
				DEFAULT_ReSIDfp_STEREO_FILTER_8580);
	}

	@Override
	public final void setReSIDfpStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Stereo_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_3rdSID_Filter8580", DEFAULT_ReSIDfp_3SID_FILTER_8580);
	}

	@Override
	public final void setReSIDfpThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_3rdSID_Filter8580", filterName);
	}

}