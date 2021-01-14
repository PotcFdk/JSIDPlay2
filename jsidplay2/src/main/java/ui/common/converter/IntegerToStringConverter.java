package ui.common.converter;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

public class IntegerToStringConverter extends StringConverter<Integer> {

	private ResourceBundle bundle;

	private String zeroBundleKey;

	public IntegerToStringConverter(ResourceBundle bundle, String zeroBundleKey) {
		this.bundle = bundle;
		this.zeroBundleKey = zeroBundleKey;
	}

	@Override
	public Integer fromString(String rating) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public String toString(Integer rating) {
		if (rating.intValue() == 0) {
			return bundle.getString(zeroBundleKey);
		}
		return String.valueOf(rating);
	}

}
