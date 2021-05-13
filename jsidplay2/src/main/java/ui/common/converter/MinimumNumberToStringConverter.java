package ui.common.converter;

import java.util.Locale;

import javafx.util.converter.NumberStringConverter;

public final class MinimumNumberToStringConverter extends NumberStringConverter {

	private final Number minimumNumber;

	public MinimumNumberToStringConverter(String pattern, Number minValue) {
		this(Locale.getDefault(), pattern, minValue);
	}

	public MinimumNumberToStringConverter(Locale locale, String pattern, Number minimumNumber) {
		super(locale, pattern);
		this.minimumNumber = minimumNumber;
	}

	@Override
	public Number fromString(String value) {
		final Number result = super.fromString(value);
		if (result.doubleValue() < minimumNumber.doubleValue()) {
			throw new RuntimeException("number must be greater or equal than " + minimumNumber);
		}
		return result;
	}

}