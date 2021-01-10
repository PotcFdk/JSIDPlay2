package ui.common.converter;

import javafx.util.StringConverter;

public final class NumberToStringConverter<T extends Number> extends StringConverter<T> {
	private int factor;
	private int decimalPlaces;

	/**
	 * @param decimalPlaces decimal places
	 */
	public NumberToStringConverter(int decimalPlaces) {
		this.decimalPlaces = decimalPlaces;
		this.factor = (int) Math.pow(10, decimalPlaces);
	}

	@Override
	public String toString(T d) {
		if (d.doubleValue() == -1) {
			return "";
		}
		double rounded = (double) Math.round(d.doubleValue() * factor) / factor;
		if (factor == 0) {
			return String.format("%f", rounded);
		} else {
			return String.format("%." + decimalPlaces + "f", rounded);
		}
	}

	@Override
	public T fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

}