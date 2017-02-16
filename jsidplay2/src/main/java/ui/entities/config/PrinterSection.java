package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_PRINTER_ON;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import libsidplay.config.IPrinterSection;

@Embeddable
@Access(AccessType.PROPERTY)
public class PrinterSection implements IPrinterSection {

	private BooleanProperty printerOnProperty = new SimpleBooleanProperty(DEFAULT_PRINTER_ON);

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
