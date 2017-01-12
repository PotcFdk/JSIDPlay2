package sidplay.consoleplayer;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import libsidutils.siddatabase.TimeConverter;

/**
 * Parse mm:ss (parse time in minutes and seconds and store as seconds)
 */
public class ParameterTimeConverter implements IStringConverter<Integer> {

	@Override
	public Integer convert(String time) {
		int seconds = new TimeConverter().fromString(time).intValue();
		if (seconds == -1) {
			throw new ParameterException("Invalid time, expected mm:ss (found " + time + ")");
		}
		return seconds;
	}

}