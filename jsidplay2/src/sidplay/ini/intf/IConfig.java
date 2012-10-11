package sidplay.ini.intf;

import java.util.List;

public interface IConfig {

	public ISidPlay2Section getSidplay2();

	public IC1541Section getC1541();

	public IPrinterSection getPrinter();

	public IJoystickSection getJoystick();

	public IConsoleSection getConsole();

	public IAudioSection getAudio();

	public IEmulationSection getEmulation();

	public IFavoritesSection getFavorites();

	public List<? extends IFilterSection> getFilter();

}