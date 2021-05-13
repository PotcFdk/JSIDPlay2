package ui.common.converter;

import java.util.ResourceBundle;

import javafx.util.converter.IntegerStringConverter;

public final class IntegerToStringConverter extends IntegerStringConverter {

	private final ResourceBundle bundle;
	private final String zeroBundleKey;

	public IntegerToStringConverter(ResourceBundle bundle, String zeroBundleKey) {
		this.bundle = bundle;
		this.zeroBundleKey = zeroBundleKey;
	}

	@Override
	public String toString(Integer integer) {
		if (integer.intValue() == 0) {
			return bundle.getString(zeroBundleKey);
		}
		return String.valueOf(integer);
	}

}
