package ui.common;

import javafx.util.StringConverter;

public final class DoubleToString extends StringConverter<Double> {
	@Override
	public String toString(Double d) {
		double rounded = (double) Math.round(d * 10) / 10;
		return String.format("%.1f", rounded);
	}

	@Override
	public Double fromString(String string) {
		return Double.parseDouble(string);
	}
}