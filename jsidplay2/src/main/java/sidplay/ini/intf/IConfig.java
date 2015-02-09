package sidplay.ini.intf;

import java.util.List;

public interface IConfig {

	/** Bump this each time you want to invalidate the configuration */
	int REQUIRED_CONFIG_VERSION = 19;

	ISidPlay2Section getSidplay2();

	IC1541Section getC1541();

	IPrinterSection getPrinter();

	IConsoleSection getConsole();

	IAudioSection getAudio();

	IEmulationSection getEmulation();

	List<? extends IFilterSection> getFilter();

}