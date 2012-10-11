package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IPrinterSection;

@Embeddable
public class DbPrinterSection implements IPrinterSection {

	private boolean isPrinterOn;

	@Override
	public boolean isPrinterOn() {
		return isPrinterOn;
	}

	@Override
	public void setPrinterOn(boolean isPrinterOn) {
		this.isPrinterOn = isPrinterOn;
	}

}
