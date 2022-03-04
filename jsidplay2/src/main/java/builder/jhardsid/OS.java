package builder.jhardsid;

import java.util.Locale;

public enum OS {
	LINUX, WINDOWS, MAC, OTHER;

	private static OS os;
	static {
		String OS = System.getProperty("os.name", "other").toLowerCase(Locale.ENGLISH);
		if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
			os = MAC;
		} else if (OS.indexOf("win") >= 0) {
			os = WINDOWS;
		} else if (OS.indexOf("nux") >= 0) {
			os = LINUX;
		} else {
			os = OTHER;
		}
	}

	public static OS get() {
		return os;
	}

	@Override
	public String toString() {
		return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
	}
}
