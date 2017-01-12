package ui.common;

import static libsidutils.siddatabase.TimeConverter.SECONDS_PER_MINUTE;

import javafx.util.converter.NumberStringConverter;
import libsidutils.siddatabase.TimeConverter;

/**
 * Convert time in minutes and seconds (mm:ss) to number (seconds)
 */
public class TimeToStringConverter extends NumberStringConverter {
	@Override
	public Number fromString(String time) {
		return new TimeConverter().fromString(time);
	}

	@Override
	public String toString(Number seconds) {
		if (seconds.intValue() == -1) {
			// Erroneous time configured last time?
			return "00:00";
		}
		return String.format("%02d:%02d", seconds.intValue() / SECONDS_PER_MINUTE,
				seconds.intValue() % SECONDS_PER_MINUTE);
	}
}
