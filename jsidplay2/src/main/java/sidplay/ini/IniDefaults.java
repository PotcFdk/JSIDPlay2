package sidplay.ini;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.config.IConfig;
import sidplay.audio.Audio;

/**
 * Provide constants for all settings read from the internal INI file.
 * 
 * @author ken
 *
 */
public interface IniDefaults {

	IConfig DEFAULTS = IniConfig.getDefault();

	Audio DEFAULT_AUDIO = DEFAULTS.getAudioSection().getAudio();
	int DEFAULT_DEVICE = DEFAULTS.getAudioSection().getDevice();
	SamplingRate DEFAULT_SAMPLING_RATE = DEFAULTS.getAudioSection().getSamplingRate();
	SamplingMethod DEFAULT_SAMPLING = DEFAULTS.getAudioSection().getSampling();
	boolean DEFAULT_PLAY_ORIGINAL = DEFAULTS.getAudioSection().isPlayOriginal();
	float DEFAULT_MAIN_VOLUME = DEFAULTS.getAudioSection().getMainVolume();
	float DEFAULT_SECOND_VOLUME = DEFAULTS.getAudioSection().getSecondVolume();
	float DEFAULT_THIRD_VOLUME = DEFAULTS.getAudioSection().getThirdVolume();
	float DEFAULT_MAIN_BALANCE = DEFAULTS.getAudioSection().getMainBalance();
	float DEFAULT_SECOND_BALANCE = DEFAULTS.getAudioSection().getSecondBalance();
	float DEFAULT_THIRD_BALANCE = DEFAULTS.getAudioSection().getThirdBalance();
	int DEFAULT_BUFFER_SIZE = DEFAULTS.getAudioSection().getBufferSize();

	boolean DEFAULT_DRIVE_ON = DEFAULTS.getC1541Section().isDriveOn();
	boolean DEFAULT_PARALLEL_CABLE = DEFAULTS.getC1541Section().isParallelCable();
	int MAX_RAM_EXPANSIONS = 5;
	boolean DEFAULT_RAM_EXPAND_0X2000 = DEFAULTS.getC1541Section().isRamExpansionEnabled0();
	boolean DEFAULT_RAM_EXPAND_0X4000 = DEFAULTS.getC1541Section().isRamExpansionEnabled1();
	boolean DEFAULT_RAM_EXPAND_0X6000 = DEFAULTS.getC1541Section().isRamExpansionEnabled2();
	boolean DEFAULT_RAM_EXPAND_0X8000 = DEFAULTS.getC1541Section().isRamExpansionEnabled3();
	boolean DEFAULT_RAM_EXPAND_0XA000 = DEFAULTS.getC1541Section().isRamExpansionEnabled4();
	FloppyType DEFAULT_FLOPPY_TYPE = DEFAULTS.getC1541Section().getFloppyType();

	Engine DEFAULT_ENGINE = DEFAULTS.getEmulationSection().getEngine();
	Emulation DEFAULT_EMULATION = DEFAULTS.getEmulationSection().getDefaultEmulation();
	Emulation DEFAULT_USER_EMULATION = DEFAULTS.getEmulationSection().getUserEmulation();
	Emulation DEFAULT_STEREO_EMULATION = DEFAULTS.getEmulationSection().getStereoEmulation();
	Emulation DEFAULT_3SID_EMULATION = DEFAULTS.getEmulationSection().getThirdEmulation();
	CPUClock DEFAULT_CLOCK_SPEED = DEFAULTS.getEmulationSection().getDefaultClockSpeed();
	ChipModel DEFAULT_SID_MODEL = DEFAULTS.getEmulationSection().getDefaultSidModel();
	ChipModel DEFAULT_USER_MODEL = DEFAULTS.getEmulationSection().getUserSidModel();
	ChipModel DEFAULT_STEREO_MODEL = DEFAULTS.getEmulationSection().getStereoSidModel();
	ChipModel DEFAULT_3SID_MODEL = DEFAULTS.getEmulationSection().getThirdSIDModel();
	int DEFAULT_HARD_SID_6581 = DEFAULTS.getEmulationSection().getHardsid6581();
	int DEFAULT_HARD_SID_8580 = DEFAULTS.getEmulationSection().getHardsid8580();
	boolean DEFAULT_USE_FILTER = DEFAULTS.getEmulationSection().isFilter();
	boolean DEFAULT_USE_STEREO_FILTER = DEFAULTS.getEmulationSection().isStereoFilter();
	boolean DEFAULT_USE_3SID_FILTER = DEFAULTS.getEmulationSection().isThirdSIDFilter();
	int DEFAULT_SID_NUM_TO_READ = DEFAULTS.getEmulationSection().getSidNumToRead();
	boolean DEFAULT_DIGI_BOOSTED_8580 = DEFAULTS.getEmulationSection().isDigiBoosted8580();
	int DEFAULT_DUAL_SID_BASE = DEFAULTS.getEmulationSection().getDualSidBase();
	int DEFAULT_THIRD_SID_BASE = DEFAULTS.getEmulationSection().getThirdSIDBase();
	boolean DEFAULT_FAKE_STEREO = DEFAULTS.getEmulationSection().isFakeStereo();
	boolean DEFAULT_FORCE_STEREO_TUNE = DEFAULTS.getEmulationSection().isForceStereoTune();
	boolean DEFAULT_FORCE_3SID_TUNE = DEFAULTS.getEmulationSection().isForce3SIDTune();

	String DEFAULT_FILTER_6581 = DEFAULTS.getEmulationSection().getFilter6581();
	String DEFAULT_STEREO_FILTER_6581 = DEFAULTS.getEmulationSection().getStereoFilter6581();
	String DEFAULT_3SID_FILTER_6581 = DEFAULTS.getEmulationSection().getThirdSIDFilter6581();

	String DEFAULT_FILTER_8580 = DEFAULTS.getEmulationSection().getFilter8580();
	String DEFAULT_STEREO_FILTER_8580 = DEFAULTS.getEmulationSection().getStereoFilter8580();
	String DEFAULT_3SID_FILTER_8580 = DEFAULTS.getEmulationSection().getThirdSIDFilter8580();

	String DEFAULT_ReSIDfp_FILTER_6581 = DEFAULTS.getEmulationSection().getReSIDfpFilter6581();
	String DEFAULT_ReSIDfp_STEREO_FILTER_6581 = DEFAULTS.getEmulationSection().getReSIDfpStereoFilter6581();
	String DEFAULT_ReSIDfp_3SID_FILTER_6581 = DEFAULTS.getEmulationSection().getReSIDfpThirdSIDFilter6581();

	String DEFAULT_ReSIDfp_FILTER_8580 = DEFAULTS.getEmulationSection().getReSIDfpFilter8580();
	String DEFAULT_ReSIDfp_STEREO_FILTER_8580 = DEFAULTS.getEmulationSection().getReSIDfpStereoFilter8580();
	String DEFAULT_ReSIDfp_3SID_FILTER_8580 = DEFAULTS.getEmulationSection().getReSIDfpThirdSIDFilter8580();

	boolean DEFAULT_PRINTER_ON = DEFAULTS.getPrinterSection().isPrinterOn();

	boolean DEFAULT_ENABLE_DATABASE = DEFAULTS.getSidplay2Section().isEnableDatabase();
	int DEFAULT_PLAY_LENGTH = DEFAULTS.getSidplay2Section().getDefaultPlayLength();
	int DEFAULT_FADE_IN_TIME = DEFAULTS.getSidplay2Section().getFadeInTime();
	int DEFAULT_FADE_OUT_TIME = DEFAULTS.getSidplay2Section().getFadeOutTime();
	boolean DEFAULT_LOOP = DEFAULTS.getSidplay2Section().isLoop();
	boolean DEFAULT_SINGLE_TRACK = DEFAULTS.getSidplay2Section().isSingle();
	boolean DEFAULT_ENABLE_PROXY = false;
	float DEFAULT_BRIGHTNESS = DEFAULTS.getSidplay2Section().getBrightness();
	float DEFAULT_CONTRAST = DEFAULTS.getSidplay2Section().getContrast();
	float DEFAULT_GAMMA = DEFAULTS.getSidplay2Section().getGamma();
	float DEFAULT_SATURATION = DEFAULTS.getSidplay2Section().getSaturation();
	float DEFAULT_PHASE_SHIFT = DEFAULTS.getSidplay2Section().getPhaseShift();
	float DEFAULT_OFFSET = DEFAULTS.getSidplay2Section().getOffset();
	float DEFAULT_TINT = DEFAULTS.getSidplay2Section().getTint();
	float DEFAULT_BLUR = DEFAULTS.getSidplay2Section().getBlur();
	float DEFAULT_BLEED = DEFAULTS.getSidplay2Section().getBleed();
	boolean DEFAULT_TURBO_TAPE = DEFAULTS.getSidplay2Section().isTurboTape();

}
