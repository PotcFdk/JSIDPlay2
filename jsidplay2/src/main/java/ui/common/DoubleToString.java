package ui.common;

import javafx.util.StringConverter;

public final class DoubleToString extends StringConverter<Double> {
	private int factor;
	private int decimalPlaces;

	/**
	 * @param decimalPlaces
	 *            decimal places
	 */
	public DoubleToString(int decimalPlaces) {
		this.decimalPlaces = decimalPlaces;
		this.factor = (int) Math.pow(10, decimalPlaces);
	}

	@Override
	public String toString(Double d) {
		double rounded = (double) Math.round(d * factor) / factor;
		return String.format("%." + decimalPlaces + "f", rounded);
	}

	@Override
	public Double fromString(String string) {
		return Double.parseDouble(string);
	}
}