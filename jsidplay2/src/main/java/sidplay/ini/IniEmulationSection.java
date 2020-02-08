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
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_0;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_1;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_2;
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
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_MODE;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_HOST;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_PORT;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_SYNC_DELAY;
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
import libsidplay.common.Ultimate64Mode;
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
	@Parameter(names = { "--engine", "-E" }, descriptionKey = "ENGINE", order = 1000)
	public void setEngine(Engine engine) {
		iniReader.setProperty("Emulation", "Engine", engine);
	}

	@Override
	public Emulation getDefaultEmulation() {
		return iniReader.getPropertyEnum("Emulation", "DefaultEmulation", DEFAULT_EMULATION, Emulation.class);
	}

	@Override
	@Parameter(names = { "--defaultEmulation", "-e" }, descriptionKey = "DEFAULT_EMULATION", order = 1001)
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
	@Parameter(names = { "--defaultClock", "-k" }, descriptionKey = "DEFAULT_CLOCK", order = 1002)
	public final void setDefaultClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "DefaultClockSpeed", speed);
	}

	@Override
	public final CPUClock getUserClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "UserClockSpeed", DEFAULT_USER_CLOCK_SPEED, CPUClock.class);
	}

	@Override
	@Parameter(names = { "--forceClock", "-c" }, descriptionKey = "FORCE_CLOCK", order = 1003)
	public final void setUserClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "UserClockSpeed", speed);
	}

	@Override
	public final ChipModel getDefaultSidModel() {
		return iniReader.getPropertyEnum("Emulation", "DefaultSidModel", DEFAULT_SID_MODEL, ChipModel.class);
	}

	@Override
	@Parameter(names = { "--defaultModel", "-u" }, descriptionKey = "DEFAULT_MODEL", order = 1004)
	public final void setDefaultSidModel(ChipModel model) {
		iniReader.setProperty("Emulation", "DefaultSidModel", model);
	}

	@Override
	public final ChipModel getUserSidModel() {
		return iniReader.getPropertyEnum("Emulation", "UserSidModel", DEFAULT_USER_MODEL, ChipModel.class);
	}

	@Override
	@Parameter(names = { "--forceModel", "-m" }, descriptionKey = "FORCE_MODEL", order = 1005)
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
	public final ChipModel getSidBlaster0Model() {
		return iniReader.getPropertyEnum("Emulation", "SIDBlaster_0", DEFAULT_SIDBLASTER_0, ChipModel.class);
	}

	@Override
	public final void setSidBlaster0Model(final ChipModel model) {
		iniReader.setProperty("Emulation", "SIDBlaster_1", model);
	}

	@Override
	public final ChipModel getSidBlaster1Model() {
		return iniReader.getPropertyEnum("Emulation", "SIDBlaster_1", DEFAULT_SIDBLASTER_1, ChipModel.class);
	}

	@Override
	public final void setSidBlaster1Model(final ChipModel model) {
		iniReader.setProperty("Emulation", "SIDBlaster_1", model);
	}

	@Override
	public final ChipModel getSidBlaster2Model() {
		return iniReader.getPropertyEnum("Emulation", "SIDBlaster_2", DEFAULT_SIDBLASTER_2, ChipModel.class);
	}

	@Override
	public final void setSidBlaster2Model(final ChipModel model) {
		iniReader.setProperty("Emulation", "SIDBlaster_2", model);
	}

	@Override
	public String getNetSIDDevHost() {
		return iniReader.getPropertyString("Emulation", "NetSIDDev Host", DEFAULT_NETSIDDEV_HOST);
	}

	@Override
	@Parameter(names = { "--NetSIDDevHost" }, descriptionKey = "NET_SID_DEV_HOST", order = 1006)
	public void setNetSIDDevHost(String hostname) {
		iniReader.setProperty("Emulation", "NetSIDDev Host", hostname);
	}

	@Override
	public int getNetSIDDevPort() {
		return iniReader.getPropertyInt("Emulation", "NetSIDDev Port", DEFAULT_NETSIDDEV_PORT);
	}

	@Override
	@Parameter(names = { "--NetSIDDevPort" }, descriptionKey = "NET_SID_DEV_PORT", order = 1007)
	public void setNetSIDDevPort(int port) {
		iniReader.setProperty("Emulation", "NetSIDDev Port", port);
	}

	@Override
	public Ultimate64Mode getUltimate64Mode() {
		return iniReader.getPropertyEnum("Emulation", "Ultimate64 Mode", DEFAULT_ULTIMATE64_MODE, Ultimate64Mode.class);
	}
	
	@Override
	@Parameter(names = { "--ultimate64Mode" }, descriptionKey = "ULTIMATE64_MODE", order = 1008)
	public void setUltimate64Mode(Ultimate64Mode ultimate64Mode) {
		iniReader.setProperty("Emulation", "Ultimate64 Mode", ultimate64Mode);
	}
	
	@Override
	public String getUltimate64Host() {
		return iniReader.getPropertyString("Emulation", "Ultimate64 Host", DEFAULT_ULTIMATE64_HOST);
	}

	@Override
	@Parameter(names = { "--Ultimate64Host" }, descriptionKey = "ULTIMATE64_HOST", order = 1009)
	public void setUltimate64Host(String hostname) {
		iniReader.setProperty("Emulation", "Ultimate64 Host", hostname);
	}

	@Override
	public int getUltimate64Port() {
		return iniReader.getPropertyInt("Emulation", "Ultimate64 Port", DEFAULT_ULTIMATE64_PORT);
	}

	@Override
	@Parameter(names = { "--Ultimate64Port" }, descriptionKey = "ULTIMATE64_PORT", order = 1010)
	public void setUltimate64Port(int port) {
		iniReader.setProperty("Emulation", "Ultimate64 Port", port);
	}

	@Override
	public int getUltimate64SyncDelay() {
		return iniReader.getPropertyInt("Emulation", "Ultimate64 Sync Delay", DEFAULT_ULTIMATE64_SYNC_DELAY);
	}

	@Override
	@Parameter(names = { "--Ultimate64SyncDelay" }, descriptionKey = "ULTIMATE64_SYNC_DELAY", order = 1011)
	public void setUltimate64SyncDelay(int syncDelay) {
		iniReader.setProperty("Emulation", "Ultimate64 Sync Delay", syncDelay);
	}


	@Override
	public final boolean isFilter() {
		return iniReader.getPropertyBool("Emulation", "UseFilter", DEFAULT_USE_FILTER);
	}

	@Override
	@Parameter(names = { "--disableFilter",
			"-i" }, descriptionKey = "DISABLE_FILTER", arity = 1, converter = NegatedBooleanConverter.class, order = 1012)
	public final void setFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseFilter", enable);
	}

	@Override
	public final boolean isStereoFilter() {
		return iniReader.getPropertyBool("Emulation", "UseStereoFilter", DEFAULT_USE_STEREO_FILTER);
	}

	@Override
	@Parameter(names = { "--disableStereoFilter",
			"-j" }, descriptionKey = "DISABLE_STEREO_FILTER", arity = 1, converter = NegatedBooleanConverter.class, order = 1013)
	public final void setStereoFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseStereoFilter", enable);
	}

	@Override
	public final boolean isThirdSIDFilter() {
		return iniReader.getPropertyBool("Emulation", "Use3rdSIDFilter", DEFAULT_USE_3SID_FILTER);
	}

	@Override
	@Parameter(names = { "--disable3rdSidFilter",
			"-J" }, descriptionKey = "DISABLE_3RD_SID_FILTER", arity = 1, converter = NegatedBooleanConverter.class, order = 1014)
	public final void setThirdSIDFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "Use3rdSIDFilter", enable);
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
	@Parameter(names = { "--digiBoosted8580" }, descriptionKey = "DIGIBOOSTED8580", arity = 1, order = 1015)
	public final void setDigiBoosted8580(final boolean boost) {
		iniReader.setProperty("Emulation", "DigiBoosted8580", boost);
	}

	@Override
	public final int getDualSidBase() {
		return iniReader.getPropertyInt("Emulation", "dualSidBase", DEFAULT_DUAL_SID_BASE);
	}

	@Override
	@Parameter(names = { "--dualSIDBase" }, descriptionKey = "DUAL_SID_BASE", arity = 1, order = 1016)
	public final void setDualSidBase(final int base) {
		iniReader.setProperty("Emulation", "dualSidBase", String.format("0x%04x", base));
	}

	@Override
	public final int getThirdSIDBase() {
		return iniReader.getPropertyInt("Emulation", "thirdSIDBase", DEFAULT_THIRD_SID_BASE);
	}

	@Override
	@Parameter(names = { "--thirdSIDBase" }, descriptionKey = "THIRD_SID_BASE", arity = 1, order = 1017)
	public final void setThirdSIDBase(final int base) {
		iniReader.setProperty("Emulation", "thirdSIDBase", String.format("0x%04x", base));
	}

	@Override
	public final boolean isFakeStereo() {
		return iniReader.getPropertyBool("Emulation", "fakeStereo", DEFAULT_FAKE_STEREO);
	}

	@Override
	@Parameter(names = { "--fakeStereo" }, descriptionKey = "FAKE_STEREO", arity = 1, order = 1018)
	public final void setFakeStereo(boolean fakeStereo) {
		iniReader.setProperty("Emulation", "fakeStereo", fakeStereo);
	}

	@Override
	public final boolean isForceStereoTune() {
		return iniReader.getPropertyBool("Emulation", "forceStereoTune", DEFAULT_FORCE_STEREO_TUNE);
	}

	@Override
	@Parameter(names = { "--dualSID", "-d" }, descriptionKey = "DUAL_SID", arity = 1, order = 1019)
	public final void setForceStereoTune(final boolean force) {
		iniReader.setProperty("Emulation", "forceStereoTune", force);
	}

	@Override
	public final boolean isForce3SIDTune() {
		return iniReader.getPropertyBool("Emulation", "force3SIDTune", DEFAULT_FORCE_3SID_TUNE);
	}

	@Override
	@Parameter(names = { "--thirdSID", "-D" }, descriptionKey = "THIRD_SID", arity = 1, order = 1020)
	public final void setForce3SIDTune(final boolean force) {
		iniReader.setProperty("Emulation", "force3SIDTune", force);
	}

	@Override
	public boolean isMuteVoice1() {
		return iniReader.getPropertyBool("Emulation", "muteVoice1", DEFAULT_MUTE_VOICE1);
	}

	@Override
	@Parameter(names = { "--muteVoice1", "-1" }, descriptionKey = "MUTE_VOICE_1", arity = 1, order = 1021)
	public void setMuteVoice1(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice1", mute);
	}

	@Override
	public boolean isMuteVoice2() {
		return iniReader.getPropertyBool("Emulation", "muteVoice2", DEFAULT_MUTE_VOICE2);
	}

	@Override
	@Parameter(names = { "--muteVoice2", "-2" }, descriptionKey = "MUTE_VOICE_2", arity = 1, order = 1022)
	public void setMuteVoice2(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice2", mute);
	}

	@Override
	public boolean isMuteVoice3() {
		return iniReader.getPropertyBool("Emulation", "muteVoice3", DEFAULT_MUTE_VOICE3);
	}

	@Override
	@Parameter(names = { "--muteVoice3", "-3" }, descriptionKey = "MUTE_VOICE_3", arity = 1, order = 1023)
	public void setMuteVoice3(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice3", mute);
	}

	@Override
	public boolean isMuteVoice4() {
		return iniReader.getPropertyBool("Emulation", "muteVoice4", DEFAULT_MUTE_VOICE4);
	}

	@Override
	@Parameter(names = { "--muteVoice4", "-4" }, descriptionKey = "MUTE_VOICE_4", arity = 1, order = 1024)
	public void setMuteVoice4(boolean mute) {
		iniReader.setProperty("Emulation", "muteVoice4", mute);
	}

	@Override
	public boolean isMuteStereoVoice1() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice1", DEFAULT_MUTE_STEREO_VOICE1);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice1", "-5" }, descriptionKey = "MUTE_VOICE_5", arity = 1, order = 1025)
	public void setMuteStereoVoice1(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice1", mute);
	}

	@Override
	public boolean isMuteStereoVoice2() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice2", DEFAULT_MUTE_STEREO_VOICE2);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice2", "-6" }, descriptionKey = "MUTE_VOICE_6", arity = 1, order = 1026)
	public void setMuteStereoVoice2(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice2", mute);
	}

	@Override
	public boolean isMuteStereoVoice3() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice3", DEFAULT_MUTE_STEREO_VOICE3);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice3", "-7" }, descriptionKey = "MUTE_VOICE_7", arity = 1, order = 1027)
	public void setMuteStereoVoice3(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice3", mute);
	}

	@Override
	public boolean isMuteStereoVoice4() {
		return iniReader.getPropertyBool("Emulation", "muteStereoVoice4", DEFAULT_MUTE_STEREO_VOICE4);
	}

	@Override
	@Parameter(names = { "--muteStereoVoice4", "-8" }, descriptionKey = "MUTE_VOICE_8", arity = 1, order = 1028)
	public void setMuteStereoVoice4(boolean mute) {
		iniReader.setProperty("Emulation", "muteStereoVoice4", mute);
	}

	@Override
	public boolean isMuteThirdSIDVoice1() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice1", DEFAULT_MUTE_THIRDSID_VOICE1);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice1", "-9" }, descriptionKey = "MUTE_VOICE_9", arity = 1, order = 1029)
	public void setMuteThirdSIDVoice1(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice1", mute);
	}

	@Override
	public boolean isMuteThirdSIDVoice2() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice2", DEFAULT_MUTE_THIRDSID_VOICE2);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice2", "-10" }, descriptionKey = "MUTE_VOICE_10", arity = 1, order = 1030)
	public void setMuteThirdSIDVoice2(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice2", mute);
	}

	@Override
	public boolean isMuteThirdSIDVoice3() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice3", DEFAULT_MUTE_THIRDSID_VOICE3);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice3", "-11" }, descriptionKey = "MUTE_VOICE_11", arity = 1, order = 1031)
	public void setMuteThirdSIDVoice3(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice3", mute);
	}

	@Override
	public boolean isMuteThirdSIDVoice4() {
		return iniReader.getPropertyBool("Emulation", "muteThirdSIDVoice4", DEFAULT_MUTE_THIRDSID_VOICE4);
	}

	@Override
	@Parameter(names = { "--muteThirdSidVoice4", "-12" }, descriptionKey = "MUTE_VOICE_12", arity = 1, order = 1032)
	public void setMuteThirdSIDVoice4(boolean mute) {
		iniReader.setProperty("Emulation", "muteThirdSIDVoice4", mute);
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
	@Parameter(names = { "--filter6581" }, descriptionKey = "FILTER_6581", order = 1033)
	public final void setFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Filter6581", filterName);
	}

	@Override
	public final String getStereoFilter6581() {
		return iniReader.getPropertyString("Emulation", "Stereo_Filter6581", DEFAULT_STEREO_FILTER_6581);
	}

	@Override
	@Parameter(names = { "--stereoFilter6581" }, descriptionKey = "STEREO_FILTER_6581", order = 1034)
	public final void setStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Stereo_Filter6581", filterName);
	}

	@Override
	public final String getThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "3rdSID_Filter6581", DEFAULT_3SID_FILTER_6581);
	}

	@Override
	@Parameter(names = { "--thirdFilter6581" }, descriptionKey = "THIRD_FILTER_6581", order = 1035)
	public final void setThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "3rdSID_Filter6581", filterName);
	}

	@Override
	public final String getFilter8580() {
		return iniReader.getPropertyString("Emulation", "Filter8580", DEFAULT_FILTER_8580);
	}

	@Override
	@Parameter(names = { "--filter8580" }, descriptionKey = "FILTER_8580", order = 1036)
	public final void setFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Filter8580", filterName);
	}

	@Override
	public final String getStereoFilter8580() {
		return iniReader.getPropertyString("Emulation", "Stereo_Filter8580", DEFAULT_STEREO_FILTER_8580);
	}

	@Override
	@Parameter(names = { "--stereoFilter8580" }, descriptionKey = "STEREO_FILTER_8580", order = 1037)
	public final void setStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Stereo_Filter8580", filterName);
	}

	@Override
	public final String getThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "3rdSID_Filter8580", DEFAULT_3SID_FILTER_8580);
	}

	@Override
	@Parameter(names = { "--thirdFilter8580" }, descriptionKey = "THIRD_FILTER_8580", order = 1038)
	public final void setThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "3rdSID_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter6581", DEFAULT_ReSIDfp_FILTER_6581);
	}

	@Override
	@Parameter(names = { "--reSIDfpFilter6581" }, descriptionKey = "RESIDFP_FILTER_6581", order = 1039)
	public final void setReSIDfpFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter6581", filterName);
	}

	@Override
	public final String getReSIDfpStereoFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Stereo_Filter6581",
				DEFAULT_ReSIDfp_STEREO_FILTER_6581);
	}

	@Override
	@Parameter(names = { "--reSIDfpStereoFilter6581" }, descriptionKey = "RESIDFP_STEREO_FILTER_6581", order = 1040)
	public final void setReSIDfpStereoFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Stereo_Filter6581", filterName);
	}

	@Override
	public final String getReSIDfpThirdSIDFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_3rdSID_Filter6581", DEFAULT_ReSIDfp_3SID_FILTER_6581);
	}

	@Override
	@Parameter(names = { "--reSIDfpThirdFilter6581" }, descriptionKey = "RESIDFP_THIRD_FILTER_6581", order = 1041)
	public final void setReSIDfpThirdSIDFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_3rdSID_Filter6581", filterName);
	}

	@Override
	public final String getReSIDfpFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter8580", DEFAULT_ReSIDfp_FILTER_8580);
	}

	@Parameter(names = { "--reSIDfpFilter8580" }, descriptionKey = "RESIDFP_FILTER_8580", order = 1042)
	public final void setReSIDfpFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpStereoFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Stereo_Filter8580",
				DEFAULT_ReSIDfp_STEREO_FILTER_8580);
	}

	@Override
	@Parameter(names = { "--reSIDfpStereoFilter8580" }, descriptionKey = "RESIDFP_STEREO_FILTER_8580", order = 1043)
	public final void setReSIDfpStereoFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Stereo_Filter8580", filterName);
	}

	@Override
	public final String getReSIDfpThirdSIDFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_3rdSID_Filter8580", DEFAULT_ReSIDfp_3SID_FILTER_8580);
	}

	@Override
	@Parameter(names = { "--reSIDfpThirdFilter8580" }, descriptionKey = "RESIDFP_THIRD_FILTER_8580", order = 1044)
	public final void setReSIDfpThirdSIDFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_3rdSID_Filter8580", filterName);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("engine=").append(getEngine()).append(",");
		result.append("defaultEmulation=").append(getDefaultEmulation()).append(",");
		result.append("userEmulation=").append(getUserEmulation()).append(",");
		result.append("stereoEmulation=").append(getStereoEmulation()).append(",");
		result.append("thirdEmulation=").append(getThirdEmulation()).append(",");
		result.append("defaultClockSpeed=").append(getDefaultClockSpeed()).append(",");
		result.append("userClockSpeed=").append(getUserClockSpeed()).append(",");
		result.append("defaultSidModel=").append(getDefaultSidModel()).append(",");
		result.append("userSidModel=").append(getUserSidModel()).append(",");
		result.append("stereoSidModel=").append(getStereoSidModel()).append(",");
		result.append("thirdSIDModel=").append(getThirdSIDModel()).append(",");
		result.append("hardsid6581=").append(getHardsid6581()).append(",");
		result.append("hardsid8580=").append(getHardsid8580()).append(",");
		result.append("netSIDDevHost=").append(getNetSIDDevHost()).append(",");
		result.append("netSIDDevPort=").append(getNetSIDDevPort()).append(",");
		result.append("ultimate64Host=").append(getUltimate64Host()).append(",");
		result.append("ultimate64Port=").append(getUltimate64Port()).append(",");
		result.append("filter=").append(isFilter()).append(",");
		result.append("stereoFilter=").append(isStereoFilter()).append(",");
		result.append("thirdSIDFilter=").append(isThirdSIDFilter()).append(",");
		result.append("sidNumToRead=").append(getSidNumToRead()).append(",");
		result.append("digiBoost8580=").append(isDigiBoosted8580()).append(",");
		result.append("dualSidBase=").append(getDualSidBase()).append(",");
		result.append("thirdSIDBase=").append(getThirdSIDBase()).append(",");
		result.append("fakeStereo=").append(isFakeStereo()).append(",");
		result.append("forceStereoTune=").append(isForceStereoTune()).append(",");
		result.append("force3SIDTume=").append(isForce3SIDTune()).append(",");
		result.append("muteVoice1=").append(isMuteVoice1()).append(",");
		result.append("muteVoice2=").append(isMuteVoice2()).append(",");
		result.append("muteVoice3=").append(isMuteVoice3()).append(",");
		result.append("muteVoice4=").append(isMuteVoice4()).append(",");
		result.append("muteStereoVoice1=").append(isMuteStereoVoice1()).append(",");
		result.append("muteStereoVoice2=").append(isMuteStereoVoice2()).append(",");
		result.append("muteStereoVoice3=").append(isMuteStereoVoice3()).append(",");
		result.append("muteStereoVoice4=").append(isMuteStereoVoice4()).append(",");
		result.append("muteThirdSIDVoice1=").append(isMuteThirdSIDVoice1()).append(",");
		result.append("muteThirdSIDVoice2=").append(isMuteThirdSIDVoice2()).append(",");
		result.append("muteThirdSIDVoice3=").append(isMuteThirdSIDVoice3()).append(",");
		result.append("muteThirdSIDVoice4=").append(isMuteThirdSIDVoice4()).append(",");
		result.append("netSIDFilter6581=").append(getNetSIDFilter6581()).append(",");
		result.append("netSIDStereoFilter6581=").append(getNetSIDStereoFilter6581()).append(",");
		result.append("netSIDThirdSIDFilter6581=").append(getNetSIDThirdSIDFilter6581()).append(",");
		result.append("netSIDFilter8580=").append(getNetSIDFilter8580()).append(",");
		result.append("netSIDStereoFilter8580=").append(getNetSIDStereoFilter8580()).append(",");
		result.append("netSIDThirdSIDFilter8580=").append(getNetSIDThirdSIDFilter8580()).append(",");
		result.append("filter6581=").append(getFilter6581()).append(",");
		result.append("stereoFilter6581=").append(getStereoFilter6581()).append(",");
		result.append("thirdSidFilter6581=").append(getThirdSIDFilter6581()).append(",");
		result.append("filter8580=").append(getFilter8580()).append(",");
		result.append("stereoFilter8580=").append(getStereoFilter8580()).append(",");
		result.append("thirdSidFilter8580=").append(getThirdSIDFilter8580()).append(",");
		result.append("reSIDfpFilter6581=").append(getReSIDfpFilter6581()).append(",");
		result.append("reSIDfpStereoFilter6581=").append(getReSIDfpStereoFilter6581()).append(",");
		result.append("reSIDfpThirdSidFilter6581=").append(getReSIDfpThirdSIDFilter6581()).append(",");
		result.append("reSIDfpFilter8580=").append(getReSIDfpFilter8580()).append(",");
		result.append("reSIDfpStereoFilter8580=").append(getReSIDfpStereoFilter8580()).append(",");
		result.append("reSIDfpThirdSidFilter8580=").append(getReSIDfpThirdSIDFilter8580());
		return result.toString();
	}
}