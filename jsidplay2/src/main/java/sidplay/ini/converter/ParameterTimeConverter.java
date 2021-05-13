package sidplay.ini.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import libsidutils.siddatabase.TimeConverter;

/**
 * Parse mm:ss.SSS (parse time in minutes and seconds and store as seconds)
 */
public final class ParameterTimeConverter implements IStringConverter<Double> {

	private final TimeConverter timeConverter = new TimeConverter();

	@Override
	public Double convert(String time) {
		double seconds = timeConverter.fromString(time).doubleValue();
		if (seconds == -1) {
			throw new ParameterException("Invalid time, expected mm:ss.SSS (found " + time + ")");
		}
		return seconds;
	}

}