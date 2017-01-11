package sidplay.consoleplayer;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import ui.common.TimeNumberStringConverter;

/**
 * Parse [mm:]ss (parse time in minutes and seconds and store as seconds)
 */
public class TimeConverter implements IStringConverter<Integer> {
	private TimeNumberStringConverter timeNumberStringConverter = new TimeNumberStringConverter();

	@Override
	public Integer convert(String time) {
		int seconds = timeNumberStringConverter.fromString(time).intValue();
		if (seconds == -1) {
			throw new ParameterException("Invalid time, expected [mm:]ss (found " + time + ")");
		}
		return seconds;
	}
}