package sidplay.ini;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.config.IAudioSection;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IPrinterSection;
import libsidplay.config.ISidPlay2Section;
import sidplay.audio.Audio;

/**
 * Provide constants for all settings read from the internal INI file.
 * 
 * @author ken
 *
 */
public interface IniDefaults {
	IConfig DEFAULTS = IniConfig.getDefault();

	// SIDPlay2 Section

	ISidPlay2Section SIDPLAY2_SECTION = DEFAULTS.getSidplay2Section();
	boolean DEFAULT_ENABLE_DATABASE = SIDPLAY2_SECTION.isEnableDatabase();
	int DEFAULT_PLAY_LENGTH = SIDPLAY2_SECTION.getDefaultPlayLength();
	int DEFAULT_FADE_IN_TIME = SIDPLAY2_SECTION.getFadeInTime();
	int DEFAULT_FADE_OUT_TIME = SIDPLAY2_SECTION.getFadeOutTime();
	boolean DEFAULT_LOOP = SIDPLAY2_SECTION.isLoop();
	boolean DEFAULT_SINGLE_TRACK = SIDPLAY2_SECTION.isSingle();
	boolean DEFAULT_ENABLE_PROXY = false;
	float DEFAULT_BRIGHTNESS = SIDPLAY2_SECTION.getBrightness();
	float DEFAULT_CONTRAST = SIDPLAY2_SECTION.getContrast();
	float DEFAULT_GAMMA = SIDPLAY2_SECTION.getGamma();
	float DEFAULT_SATURATION = SIDPLAY2_SECTION.getSaturation();
	float DEFAULT_PHASE_SHIFT = SIDPLAY2_SECTION.getPhaseShift();
	float DEFAULT_OFFSET = SIDPLAY2_SECTION.getOffset();
	float DEFAULT_TINT = SIDPLAY2_SECTION.getTint();
	float DEFAULT_BLUR = SIDPLAY2_SECTION.getBlur();
	float DEFAULT_BLEED = SIDPLAY2_SECTION.getBleed();
	boolean DEFAULT_TURBO_TAPE = SIDPLAY2_SECTION.isTurboTape();

	// Audio Section

	IAudioSection AUDIO_SECTION = DEFAULTS.getAudioSection();
	Audio DEFAULT_AUDIO = AUDIO_SECTION.getAudio();
	int DEFAULT_DEVICE = AUDIO_SECTION.getDevice();
	SamplingRate DEFAULT_SAMPLING_RATE = AUDIO_SECTION.getSamplingRate();
	SamplingMethod DEFAULT_SAMPLING = AUDIO_SECTION.getSampling();
	boolean DEFAULT_PLAY_ORIGINAL = AUDIO_SECTION.isPlayOriginal();
	float DEFAULT_MAIN_VOLUME = AUDIO_SECTION.getMainVolume();
	float DEFAULT_SECOND_VOLUME = AUDIO_SECTION.getSecondVolume();
	float DEFAULT_THIRD_VOLUME = AUDIO_SECTION.getThirdVolume();
	float DEFAULT_MAIN_BALANCE = AUDIO_SECTION.getMainBalance();
	float DEFAULT_SECOND_BALANCE = AUDIO_SECTION.getSecondBalance();
	float DEFAULT_THIRD_BALANCE = AUDIO_SECTION.getThirdBalance();
	int DEFAULT_BUFFER_SIZE = AUDIO_SECTION.getBufferSize();

	// Printer Section

	IPrinterSection PRINTER_SECTION = DEFAULTS.getPrinterSection();
	boolean DEFAULT_PRINTER_ON = PRINTER_SECTION.isPrinterOn();

	// C1541 Section

	IC1541Section C1541_SECTION = DEFAULTS.getC1541Section();
	boolean DEFAULT_DRIVE_ON = C1541_SECTION.isDriveOn();
	boolean DEFAULT_PARALLEL_CABLE = C1541_SECTION.isParallelCable();
	int MAX_RAM_EXPANSIONS = 5;
	boolean DEFAULT_RAM_EXPAND_0X2000 = C1541_SECTION.isRamExpansionEnabled0();
	boolean DEFAULT_RAM_EXPAND_0X4000 = C1541_SECTION.isRamExpansionEnabled1();
	boolean DEFAULT_RAM_EXPAND_0X6000 = C1541_SECTION.isRamExpansionEnabled2();
	boolean DEFAULT_RAM_EXPAND_0X8000 = C1541_SECTION.isRamExpansionEnabled3();
	boolean DEFAULT_RAM_EXPAND_0XA000 = C1541_SECTION.isRamExpansionEnabled4();
	FloppyType DEFAULT_FLOPPY_TYPE = C1541_SECTION.getFloppyType();

	// Emulator Section

	IEmulationSection EMULATOR_SECTION = DEFAULTS.getEmulationSection();
	Engine DEFAULT_ENGINE = EMULATOR_SECTION.getEngine();
	Emulation DEFAULT_EMULATION = EMULATOR_SECTION.getDefaultEmulation();
	Emulation DEFAULT_USER_EMULATION = EMULATOR_SECTION.getUserEmulation();
	Emulation DEFAULT_STEREO_EMULATION = EMULATOR_SECTION.getStereoEmulation();
	Emulation DEFAULT_3SID_EMULATION = EMULATOR_SECTION.getThirdEmulation();
	CPUClock DEFAULT_CLOCK_SPEED = EMULATOR_SECTION.getDefaultClockSpeed();
	ChipModel DEFAULT_SID_MODEL = EMULATOR_SECTION.getDefaultSidModel();
	ChipModel DEFAULT_USER_MODEL = EMULATOR_SECTION.getUserSidModel();
	ChipModel DEFAULT_STEREO_MODEL = EMULATOR_SECTION.getStereoSidModel();
	ChipModel DEFAULT_3SID_MODEL = EMULATOR_SECTION.getThirdSIDModel();
	int DEFAULT_HARD_SID_6581 = EMULATOR_SECTION.getHardsid6581();
	int DEFAULT_HARD_SID_8580 = EMULATOR_SECTION.getHardsid8580();
	boolean DEFAULT_USE_FILTER = EMULATOR_SECTION.isFilter();
	boolean DEFAULT_USE_STEREO_FILTER = EMULATOR_SECTION.isStereoFilter();
	boolean DEFAULT_USE_3SID_FILTER = EMULATOR_SECTION.isThirdSIDFilter();
	int DEFAULT_SID_NUM_TO_READ = EMULATOR_SECTION.getSidNumToRead();
	boolean DEFAULT_DIGI_BOOSTED_8580 = EMULATOR_SECTION.isDigiBoosted8580();
	int DEFAULT_DUAL_SID_BASE = EMULATOR_SECTION.getDualSidBase();
	int DEFAULT_THIRD_SID_BASE = EMULATOR_SECTION.getThirdSIDBase();
	boolean DEFAULT_FAKE_STEREO = EMULATOR_SECTION.isFakeStereo();
	boolean DEFAULT_FORCE_STEREO_TUNE = EMULATOR_SECTION.isForceStereoTune();
	boolean DEFAULT_FORCE_3SID_TUNE = EMULATOR_SECTION.isForce3SIDTune();
	String DEFAULT_FILTER_6581 = EMULATOR_SECTION.getFilter6581();
	String DEFAULT_STEREO_FILTER_6581 = EMULATOR_SECTION.getStereoFilter6581();
	String DEFAULT_3SID_FILTER_6581 = EMULATOR_SECTION.getThirdSIDFilter6581();
	String DEFAULT_FILTER_8580 = EMULATOR_SECTION.getFilter8580();
	String DEFAULT_STEREO_FILTER_8580 = EMULATOR_SECTION.getStereoFilter8580();
	String DEFAULT_3SID_FILTER_8580 = EMULATOR_SECTION.getThirdSIDFilter8580();
	String DEFAULT_ReSIDfp_FILTER_6581 = EMULATOR_SECTION.getReSIDfpFilter6581();
	String DEFAULT_ReSIDfp_STEREO_FILTER_6581 = EMULATOR_SECTION.getReSIDfpStereoFilter6581();
	String DEFAULT_ReSIDfp_3SID_FILTER_6581 = EMULATOR_SECTION.getReSIDfpThirdSIDFilter6581();
	String DEFAULT_ReSIDfp_FILTER_8580 = EMULATOR_SECTION.getReSIDfpFilter8580();
	String DEFAULT_ReSIDfp_STEREO_FILTER_8580 = EMULATOR_SECTION.getReSIDfpStereoFilter8580();
	String DEFAULT_ReSIDfp_3SID_FILTER_8580 = EMULATOR_SECTION.getReSIDfpThirdSIDFilter8580();
}
