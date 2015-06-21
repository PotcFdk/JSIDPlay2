package ui.entities.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.persistence.Embeddable;

import libsidplay.config.IPrinterSection;

@Embeddable
public class PrinterSection implements IPrinterSection {

	private BooleanProperty printerOnProperty = new SimpleBooleanProperty(
			DEFAULT_PRINTER_ON);

	@Override
	public boolean isPrinterOn() {
		return printerOnProperty.get();
	}

	@Override
	public void setPrinterOn(boolean printerOn) {
		printerOnProperty.set(printerOn);
	}

	public BooleanProperty printerOnProperty() {
		return printerOnProperty;
	}

}
