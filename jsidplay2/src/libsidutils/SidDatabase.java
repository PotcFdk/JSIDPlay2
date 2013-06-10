package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidplay.sidtune.SidTune;
import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ini.IniReader;

public class SidDatabase {
	private static final Pattern TIME_VALUE = Pattern.compile("([0-9]{1,2}):([0-9]{2})(?:\\(.*)?");

	private final IniReader database;

	private static SidDatabase theSLDB;
	private static File theHVSCRoot;
	
	public static SidDatabase getInstance(final File hvsc) {
		if (theSLDB == null && hvsc != null && !hvsc.equals(theHVSCRoot)) {
			try {
				theSLDB = AccessController
						.doPrivileged(new PrivilegedExceptionAction<SidDatabase>() {

							@Override
							public SidDatabase run() throws Exception {
								File sldbFile = getSLDBFilename(hvsc);
								if (sldbFile != null && sldbFile.exists()) {
									return new SidDatabase(sldbFile);
								}
								return null;
							}
						});
				if (theSLDB != null) {
					theHVSCRoot = hvsc;
				}
			} catch (PrivilegedActionException e) {
				// Only "checked" exceptions will be "wrapped" in a
				// PrivilegedActionException.
				e.getException().printStackTrace();
			}
		}
		return theSLDB;
	}

	protected SidDatabase(final File file) {
		try {
			if (file instanceof ZipEntryFileProxy) {
				database = new IniReader(
						((ZipEntryFileProxy) file).getInputStream());
			} else {
				database = new IniReader(new FileInputStream(file));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int parseTimeStamp(final String arg) {
		Matcher m = TIME_VALUE.matcher(arg);
		if (! m.matches()) {
			System.out.println("Failed to parse: " + arg);
			return 0;
		}
		
		return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
	}
	
	public int length(final SidTune tune) {
		final int song = tune.getInfo().currentSong;
		if (song == 0) {
			return -1;
		}
		final String md5 = tune.getMD5Digest();
		if (md5 == null) {
			return 0;
		}
		return length(md5, song);
	}

	public int length(final String md5, final int song) {
		final String value = database.getPropertyString("Database", md5, null);
		if (value == null) {
			return 0;
		}

		String[] times = value.split(" ");
		return parseTimeStamp(times[song - 1]);
	}

	protected static File getSLDBFilename(File hvscRoot) {
		List<File> docs = PathUtils.getFiles("DOCUMENTS/Songlengths.txt", hvscRoot,
				null);
		if (docs.size() > 0) {
			return docs.get(docs.size() - 1);
		}
		return null;
	}

	public int getFullSongLength(final SidTune currentTune) {
		int length = 0;
		final String md5 = currentTune.getMD5Digest();
		for (int i = 1; i <= currentTune.getInfo().songs; i++) {
			int curr_length = length(md5, i);
			length += curr_length;
		}
		return length;
	}

}
