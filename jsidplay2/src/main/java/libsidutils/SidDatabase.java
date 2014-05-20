package libsidutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidplay.sidtune.SidTune;
import sidplay.ini.IniReader;

public class SidDatabase {
	public static final String SONGLENGTHS_FILE = "DOCUMENTS/Songlengths.txt";
	private static final Pattern TIME_VALUE = Pattern
			.compile("([0-9]{1,2}):([0-9]{2})(?:\\(.*)?");

	private final IniReader database;

	public SidDatabase(final InputStream input) throws IOException {
		database = new IniReader(input);
	}

	private int parseTimeStamp(final String arg) {
		Matcher m = TIME_VALUE.matcher(arg);
		if (!m.matches()) {
			System.out.println("Failed to parse: " + arg);
			return 0;
		}

		return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
	}

	public int getFullSongLength(final SidTune tune) {
		int length = 0;
		final String md5 = tune.getMD5Digest();
		for (int i = 1; i <= tune.getInfo().getSongs(); i++) {
			length += length(md5, i);
		}
		return length;
	}

	public int length(final SidTune tune) {
		final int song = tune.getInfo().getCurrentSong();
		if (song == 0) {
			return -1;
		}
		final String md5 = tune.getMD5Digest();
		if (md5 == null) {
			return 0;
		}
		return length(md5, song);
	}

	private int length(final String md5, final int song) {
		final String value = database.getPropertyString("Database", md5, null);
		return value != null ? parseTimeStamp(value.split(" ")[song - 1]) : 0;
	}

	public String getPath(final SidTune tune) {
		final String md5 = tune.getMD5Digest();
		return md5 != null ? getPath(md5) : "";
	}

	private String getPath(final String md5) {
		final String value = database.getPropertyString("Database", "_" + md5,
				null);
		return value != null ? value.substring(1).trim() : "";
	}
}
