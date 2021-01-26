package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_PRINTER_ON;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import libsidplay.config.IPrinterSection;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Embeddable
@Access(AccessType.PROPERTY)
public class PrinterSection implements IPrinterSection {

	private ShadowField<BooleanProperty, Boolean> printerOn = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PRINTER_ON);

	@Override
	public boolean isPrinterOn() {
		return printerOn.get();
	}

	@Override
	public void setPrinterOn(boolean printerOn) {
		this.printerOn.set(printerOn);
	}

	public BooleanProperty printerOnProperty() {
		return printerOn.property();
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
