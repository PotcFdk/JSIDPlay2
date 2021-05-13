package ui.common.converter;

import java.util.ResourceBundle;

import javafx.util.converter.IntegerStringConverter;

public class IntegerToStringConverter extends IntegerStringConverter {

	private final ResourceBundle bundle;
	private final String zeroBundleKey;

	public IntegerToStringConverter(ResourceBundle bundle, String zeroBundleKey) {
		this.bundle = bundle;
		this.zeroBundleKey = zeroBundleKey;
	}

	@Override
	public String toString(Integer rating) {
		if (rating.intValue() == 0) {
			return bundle.getString(zeroBundleKey);
		}
		return String.valueOf(rating);
	}

}
