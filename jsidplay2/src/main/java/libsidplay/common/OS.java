package libsidplay.common;

import java.util.Locale;

public enum OS {
	LINUX, WINDOWS, MAC, OTHER;

	public static OS get() {
		String OS = System.getProperty("os.name", "other").toLowerCase(Locale.ENGLISH);
		if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
			return MAC;
		} else if (OS.indexOf("win") >= 0) {
			return WINDOWS;
		} else if (OS.indexOf("nux") >= 0) {
			return LINUX;
		}
		return OTHER;
	}
}
