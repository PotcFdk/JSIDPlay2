package ui.common.converter;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

public class EnumToStringConverter<T extends Enum<?>> extends StringConverter<T> {

	private final ResourceBundle bundle;

	public EnumToStringConverter(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public T fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(T object) {
		return object != null ? bundle.getString(object.name()) : null;
	}

}
