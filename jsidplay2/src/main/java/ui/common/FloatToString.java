package ui.common;

import javafx.util.StringConverter;

public final class FloatToString extends StringConverter<Float> {
	private int factor;
	private int decimalPlaces;

	/**
	 * @param decimalPlaces
	 *            decimal places
	 */
	public FloatToString(int decimalPlaces) {
		this.decimalPlaces = decimalPlaces;
		this.factor = (int) Math.pow(10, decimalPlaces);
	}

	@Override
	public String toString(Float d) {
		double rounded = (double) Math.round(d * factor) / factor;
		return String.format("%." + decimalPlaces + "f", rounded);
	}

	@Override
	public Float fromString(String string) {
		return Float.parseFloat(string);
	}
}