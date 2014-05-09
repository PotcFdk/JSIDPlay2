package sidplay.consoleplayer;

import com.beust.jcommander.IStringConverter;

/**
 * Parse [mm:]ss (parse time in minutes and seconds)
 */
public class TimeConverter implements IStringConverter<Integer> {
	@Override
	public Integer convert(String value) {
		String[] s = value.split(":");
		if (s.length == 1) {
			return Integer.parseInt(s[0]);

		} else if (s.length == 2) {
			return Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
		}
		return 0;
	}
}