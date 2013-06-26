package sidplay.ini.intf;

public interface IPrinterSection {

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