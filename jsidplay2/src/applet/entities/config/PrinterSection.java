package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IPrinterSection;

@Embeddable
public class PrinterSection implements IPrinterSection {

	private boolean printerOn;

	@Override
	public boolean isPrinterOn() {
		return printerOn;
	}

	@Override
	public void setPrinterOn(boolean isPrinterOn) {
		this.printerOn = isPrinterOn;
	}

}
