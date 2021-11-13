package ui.common.converter;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

public final class EnumToStringConverter<T extends Enum<?>> extends StringConverter<T> {

	private final ResourceBundle bundle;
	private final String nullBundleKey;

	public EnumToStringConverter(ResourceBundle bundle) {
		this(bundle, null);
	}

	public EnumToStringConverter(ResourceBundle bundle, String nullBundleKey) {
		this.bundle = bundle;
		this.nullBundleKey = nullBundleKey;
	}

	@Override
	public T fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(T object) {
		return object != null ? bundle.getString(object.name())
				: nullBundleKey != null ? bundle.getString(nullBundleKey) : null;
	}

}
