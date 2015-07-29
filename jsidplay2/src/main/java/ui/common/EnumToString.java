package ui.common;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

public class EnumToString<T extends Enum<?>> extends StringConverter<T> {

	private final ResourceBundle bundle;

	public EnumToString(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public T fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(T object) {
		return bundle.getString(object.name());
	}

}
