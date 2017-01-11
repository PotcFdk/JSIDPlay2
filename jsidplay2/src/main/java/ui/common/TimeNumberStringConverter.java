package ui.common;

import java.util.IllegalFormatException;

import javafx.util.converter.NumberStringConverter;

/**
 * Convert time in minutes and seconds ([mm:]ss) to number and vice-versa
 */
public class TimeNumberStringConverter extends NumberStringConverter {

	@Override
	public Number fromString(String value) {
		try {
			String[] s = value.split(":");
			if (s.length == 1) {
				return Integer.parseInt(s[0]);
			} else if (s.length == 2) {
				return Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
			}
		} catch (NumberFormatException e) {
		}
		return -1;
	}

	@Override
	public String toString(Number time) {
		if (time.intValue() == -1) {
			return "00:00";
		}
		try {
			return String.format("%02d:%02d", time.intValue() / 60, time.intValue() % 60);
		} catch (IllegalFormatException e) {
			return "00:00";
		}
	}
}
