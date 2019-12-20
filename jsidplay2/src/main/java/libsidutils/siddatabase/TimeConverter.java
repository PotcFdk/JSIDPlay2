package libsidutils.siddatabase;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert time in minutes and seconds (mm:ss[.SSS])
 */
public class TimeConverter {

	/**
	 * Song length.<BR>
	 * Syntax: "min:sec.millis(attribute)"<BR>
	 * e.g. "0:16(M)" for explanations please refer to file
	 * "DOCUMENTS/Songlengths.faq" contained in HVSC. Attributes in braces are only
	 * used until version HVSC#67.
	 */
	private static final Pattern TIME_VALUE = Pattern.compile("([0-9]{1,2}):([0-9]{2})(\\.[0-9]{1,3})?(?:\\(.*)?");
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

	/**
	 * Convert time in minutes and seconds (mm:ss.SSS)
	 * 
	 * @param time time string
	 * @return seconds or -1 (invalid format)
	 */
	public Double fromString(String time) {
		try {
			Matcher m = TIME_VALUE.matcher(time);
			if (!m.matches()) {
				return -1.;
			}
			int minutes = Integer.valueOf(m.group(1));
			int seconds = Integer.valueOf(m.group(2));
			int millis = m.group(3) != null ? (int) (NUMBER_FORMAT.parse(m.group(3)).doubleValue() * 1000) : 0;

			return new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).set(Calendar.MINUTE, minutes)
					.set(Calendar.SECOND, seconds).set(Calendar.MILLISECOND, millis).build().getTime().getTime()
					/ 1000.;
		} catch (NumberFormatException | ParseException e) {
			return -1.;
		}
	}
}