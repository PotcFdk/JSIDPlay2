package ui.common;

import java.text.NumberFormat;
import java.text.ParseException;

import javafx.util.StringConverter;

public final class PositiveNumberToStringConverter<T extends Number> extends StringConverter<T> {
	private int minValue;
	private boolean powerOfTwo;

	/**
	 * @param minValue minimum number value
	 */
	public PositiveNumberToStringConverter(int minValue, boolean powerOfTwo) {
		this.minValue = minValue;
		this.powerOfTwo = powerOfTwo;
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
			if (powerOfTwo && (number.intValue() & (number.intValue() - 1)) != 0) {
				throw new ParseException("number must be a power of two", minValue);
			}
			return (T) number;
		} catch (ParseException e) {
			return (T) Integer.valueOf(-1);
		}
	}

}