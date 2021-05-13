package ui.common.converter;

import javafx.util.converter.NumberStringConverter;
import libsidutils.siddatabase.TimeConverter;

/**
 * Convert time in minutes and seconds (mm:ss.SSS) to number (seconds)
 */
public final class TimeToStringConverter extends NumberStringConverter {

	private final TimeConverter timeConverter = new TimeConverter();

	@Override
	public Number fromString(String string) {
		return timeConverter.fromString(string);
	}

	@Override
	public String toString(Number number) {
		return timeConverter.toString(number.doubleValue());
	}
}
