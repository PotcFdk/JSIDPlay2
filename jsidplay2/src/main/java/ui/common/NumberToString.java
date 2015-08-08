package ui.common;

import javafx.util.StringConverter;

public final class NumberToString<T extends Number> extends StringConverter<T> {
	private int factor;
	private int decimalPlaces;

	/**
	 * @param decimalPlaces
	 *            decimal places
	 */
	public NumberToString(int decimalPlaces) {
		this.decimalPlaces = decimalPlaces;
		this.factor = (int) Math.pow(10, decimalPlaces);
	}

	@Override
	public String toString(T d) {
		double rounded = (double) Math.round(d.doubleValue() * factor) / factor;
		return String.format("%." + decimalPlaces + "f", rounded);
	}

	@Override
	public T fromString(String string) {
		throw new RuntimeException(
				"Unsupported conversion of string to number: " + string);
	}

}