package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IPrinterSection;
import applet.config.annotations.ConfigDescription;

@Embeddable
public class PrinterSection implements IPrinterSection {

	@ConfigDescription(descriptionKey = "PRINTER_PRINTER_ON_DESC", toolTipKey = "PRINTER_PRINTER_ON_TOOLTIP")
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
