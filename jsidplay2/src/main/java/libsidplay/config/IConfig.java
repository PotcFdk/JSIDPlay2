package libsidplay.config;

import java.util.List;

public interface IConfig {

	/** Bump this each time you want to invalidate the configuration */
	int REQUIRED_CONFIG_VERSION = 23;

	ISidPlay2Section getSidplay2Section();

	IC1541Section getC1541Section();

	IPrinterSection getPrinterSection();

	IAudioSection getAudioSection();

	IEmulationSection getEmulationSection();

	IWhatsSidSection getWhatsSidSection();

	List<? extends IFilterSection> getFilterSection();

}