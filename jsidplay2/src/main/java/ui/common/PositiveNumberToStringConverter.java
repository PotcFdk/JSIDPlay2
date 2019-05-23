package ui.common;

import java.text.NumberFormat;
import java.text.ParseException;

import javafx.util.StringConverter;

public final class PositiveNumberToStringConverter<T extends Number> extends StringConverter<T> {
	private int minValue;

	/**
	 * @param minValue minimum number value
	 */
	public PositiveNumberToStringConverter(int minValue) {
		this.minValue = minValue;
	}

	@Override
	public String toString(T d) {
		if (d.doubleValue() == -1) {
			return "";
		}
		return String.format("%d", d.intValue());
	}

	@Override
	@SuppressWarnings("unchecked")
	public T fromString(String string) {
		try {
			Number number = NumberFormat.getInstance().parse(string);
			if (number.doubleValue() < minValue) {
				throw new ParseException("number must be greater than", minValue);
			}
			return (T) number;
		} catch (ParseException e) {
			return (T) Integer.valueOf(-1);
		}
	}

}