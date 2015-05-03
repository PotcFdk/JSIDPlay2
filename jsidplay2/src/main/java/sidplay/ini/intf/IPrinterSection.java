package sidplay.ini.intf;

public interface IPrinterSection {
	static final boolean DEFAULT_PRINTER_ON = false;

	/**
	 * Printer turned on?
	 * 
	 * @return printer turned on?
	 */
	boolean isPrinterOn();

	/**
	 * Turn printer on.
	 * 
	 * @param on
	 *            printer turned on?
	 */
	void setPrinterOn(boolean on);

}