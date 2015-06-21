package libsidplay.config;

import java.util.List;

public interface IConfig {

	/** Bump this each time you want to invalidate the configuration */
	int REQUIRED_CONFIG_VERSION = 19;

	ISidPlay2Section getSidplay2Section();

	IC1541Section getC1541Section();

	IPrinterSection getPrinterSection();

	IAudioSection getAudioSection();

	IEmulationSection getEmulationSection();

	List<? extends IFilterSection> getFilterSection();

}