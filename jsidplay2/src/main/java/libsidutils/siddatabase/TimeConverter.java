package libsidutils.siddatabase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert time in minutes and seconds ([mm:]ss)
 */
public class TimeConverter {
	/**
	 * Song length.<BR>
	 * Syntax: "min:sec(attribute)"<BR>
	 * e.g. "0:16(M)" for explanations please refer to file
	 * "DOCUMENTS/Songlengths.faq" contained in HVSC.
	 */
	private static final Pattern TIME_VALUE = Pattern.compile("([0-9]{1,2}):([0-9]{2})(?:\\(.*)?");

	public static final int SECONDS_PER_MINUTE = 60;

	/**
	 * Convert time in minutes and seconds (mm:ss)
	 * 
	 * @param time
	 *            time string
	 * @return seconds or -1 (invalid format)
	 */
	public Integer fromString(String time) {
		try {
			Matcher m = TIME_VALUE.matcher(time);
			if (!m.matches()) {
				return -1;
			}
			return Integer.parseInt(m.group(1)) * SECONDS_PER_MINUTE + Integer.parseInt(m.group(2));
		} catch (NumberFormatException e) {
		}
		return -1;
	}
}