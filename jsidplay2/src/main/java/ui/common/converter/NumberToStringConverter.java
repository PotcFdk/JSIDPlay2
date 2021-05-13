package ui.common.converter;

import javafx.util.StringConverter;

public final class NumberToStringConverter<T extends Number> extends StringConverter<T> {

	private final int factor;
	private final int decimalPlaces;

	/**
	 * @param decimalPlaces decimal places
	 */
	public NumberToStringConverter(int decimalPlaces) {
		this.decimalPlaces = decimalPlaces;
		this.factor = (int) Math.pow(10, decimalPlaces);
	}

	@Override
	public T fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(T d) {
		double rounded = (double) Math.round(d.doubleValue() * factor) / factor;
		return String.format("%." + decimalPlaces + "f", rounded);
	}

}