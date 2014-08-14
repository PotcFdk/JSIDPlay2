package ui.entities.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IPrinterSection;

@Embeddable
public class PrinterSection implements IPrinterSection {

	private BooleanProperty printerOnProperty = new SimpleBooleanProperty();
	
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
