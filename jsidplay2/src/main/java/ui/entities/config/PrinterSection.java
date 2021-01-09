package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_PRINTER_ON;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import libsidplay.config.IPrinterSection;
import ui.common.ShadowField;

@Embeddable
@Access(AccessType.PROPERTY)
public class PrinterSection implements IPrinterSection {

	private ShadowField<BooleanProperty, Boolean> printerOnProperty = new ShadowField<>(DEFAULT_PRINTER_ON,
			SimpleBooleanProperty::new);

	@Override
	public boolean isPrinterOn() {
		return printerOnProperty.get();
	}

	@Override
	public void setPrinterOn(boolean printerOn) {
		printerOnProperty.set(printerOn);
	}

	public BooleanProperty printerOnProperty() {
		return printerOnProperty.property();
	}

}
