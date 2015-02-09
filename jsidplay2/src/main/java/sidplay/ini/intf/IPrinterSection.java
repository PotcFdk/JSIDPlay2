package sidplay.ini.intf;

public interface IPrinterSection {
	public static final boolean DEFAULT_PRINTER_ON=false;

	/**
	 * Printer turned on?
	 * 
	 * @return printer turned on?
	 */
	public boolean isPrinterOn();

	/**
	 * Turn printer on.
	 * 
	 * @param on
	 *            printer turned on?
	 */
	public void setPrinterOn(boolean on);

}