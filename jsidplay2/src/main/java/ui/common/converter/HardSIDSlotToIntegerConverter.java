package ui.common.converter;

import java.util.ResourceBundle;

import javafx.util.converter.IntegerStringConverter;

public class HardSIDSlotToIntegerConverter extends IntegerStringConverter {

	private static final String HARDSID_SLOT = "HARDSID_";

	private final ResourceBundle bundle;

	public HardSIDSlotToIntegerConverter(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public String toString(Integer integer) {
		return bundle.getString(HARDSID_SLOT + String.valueOf(integer));
	}
}
