package ui.common.converter;

import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.util.converter.NumberStringConverter;
import libsidutils.siddatabase.TimeConverter;

/**
 * Convert time in minutes and seconds (mm:ss.SSS) to number (seconds)
 */
public class TimeToStringConverter extends NumberStringConverter {
	@Override
	public Number fromString(String time) {
		return new TimeConverter().fromString(time);
	}

	@Override
	public String toString(Number seconds) {
		if (seconds.doubleValue() == -1) {
			// Erroneous time configured last time?
			return "00:00";
		}
		return new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (seconds.doubleValue() * 1000)));
	}
}
