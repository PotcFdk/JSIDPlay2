package libsidplay.config;

public interface IPrinterSection {

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