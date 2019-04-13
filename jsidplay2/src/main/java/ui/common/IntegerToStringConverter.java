package ui.common;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

public class IntegerToStringConverter extends StringConverter<Integer> {

	private ResourceBundle bundle;

	public IntegerToStringConverter(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public Integer fromString(String rating) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public String toString(Integer rating) {
		if (rating.intValue() == 0) {
			return bundle.getString("ALL_CONTENT");
		}
		return String.valueOf(rating);
	}

}
