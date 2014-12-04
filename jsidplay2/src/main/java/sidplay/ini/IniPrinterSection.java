package sidplay.ini;

import sidplay.ini.intf.IPrinterSection;

/**
 * C1541 section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniPrinterSection extends IniSection implements IPrinterSection {

	protected IniPrinterSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Printer turned on?
	 * 
	 * @return printer turned on?
	 */
	@Override
	public final boolean isPrinterOn() {
		return iniReader.getPropertyBool("Printer", "PrinterOn", false);
	}

	/**
	 * Turn printer on.
	 * 
	 * @param on
	 *            printer turned on?
	 */
	@Override
	public final void setPrinterOn(final boolean on) {
		iniReader.setProperty("Printer", "PrinterOn", on);
	}

}
