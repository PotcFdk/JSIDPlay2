package ui.entities.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

import sidplay.ini.intf.IPrinterSection;

@Embeddable
public class PrinterSection implements IPrinterSection {

	private boolean printerOn;
	
	@Transient
	private BooleanProperty printerOnProperty;
	
	@Override
	public boolean isPrinterOn() {
		if (printerOnProperty == null) {
			printerOnProperty = new SimpleBooleanProperty();
			printerOnProperty.set(printerOn);
		}
		return printerOnProperty.get();
	}

	@Override
	public void setPrinterOn(boolean printerOn) {
		isPrinterOn();
		printerOnProperty.set(printerOn);
		this.printerOn = printerOn;
	}

	public BooleanProperty printerOnProperty() {
		isPrinterOn();
		return printerOnProperty;
	}
	
}
