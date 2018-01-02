package libsidutils.siddatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import de.schlichtherle.truezip.file.TFile;
import libsidplay.sidtune.MD5Method;
import libsidplay.sidtune.SidTune;
import sidplay.ini.IniReader;

/**
 * Utility class to determine song length for tunes based on HVSC file
 * "DOCUMENTS/Songlengths.txt".
 * 
 * @author ken
 *
 */
public class SidDatabase {
	/**
	 * Until version HVSC#67
	 */
	private static final String SONGLENGTHS_FILE_TXT = "DOCUMENTS/Songlengths.txt";
	/**
	 * Since version HVSC#68
	 */
	private static final String SONGLENGTHS_FILE_MD5 = "DOCUMENTS/Songlengths.md5";

	private static Constructor<?> TFILE_IS = null;

	static {
		// support for files contained in a ZIP (optionally in the classpath)
		try {
			TFILE_IS = (Constructor<?>) Class.forName("de.schlichtherle.truezip.file.TFileInputStream")
					.getConstructor(File.class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
		}
	}

	private MD5Method version;
	private final IniReader database;

	public SidDatabase(final String hvscRoot) throws IOException {
		try (InputStream is = getInputStreamAndSetVersion(hvscRoot)) {
			database = new IniReader(is);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IOException(e.getMessage());
		}
	}

	private InputStream getInputStreamAndSetVersion(String hvscRoot)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, FileNotFoundException {
		File file = getFile(hvscRoot, SONGLENGTHS_FILE_TXT);
		version = MD5Method.MD5_PSID_HEADER;
		File songLengthFileMd5 = getFile(hvscRoot, SONGLENGTHS_FILE_MD5);
		if (songLengthFileMd5.exists() && songLengthFileMd5.canRead()) {
			file = songLengthFileMd5;
			version = MD5Method.MD5_CONTENTS;
		}
		return getInputStream(file);
	}

	private InputStream getInputStream(File file)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, FileNotFoundException {
		return TFILE_IS != null ? (InputStream) TFILE_IS.newInstance(file) : new FileInputStream(file);
	}

	private File getFile(String hvscRoot, String songLengthFilename) {
		return TFILE_IS != null ? new TFile(hvscRoot, songLengthFilename) : new File(hvscRoot, songLengthFilename);
	}

	/**
	 * Get tune length (sum of all song length contained in the tune) in seconds.
	 * 
	 * @param tune
	 *            tune to get the length for
	 * @return tune length in seconds
	 */
	public int getTuneLength(final SidTune tune) {
		int length = 0;
		final String md5 = tune.getMD5Digest(version);
		for (int songNum = 1; songNum <= tune.getInfo().getSongs(); songNum++) {
			length += getLength(md5, songNum);
		}
		return length;
	}

	/**
	 * Get song length of the current song in seconds.
	 * 
	 * @param tune
	 *            tune to determine the current song
	 * @return song length of the current song of the tune
	 */
	public int getSongLength(final SidTune tune) {
		final int songNum = tune.getInfo().getCurrentSong();
		final String md5 = tune.getMD5Digest(version);
		return songNum == 0 || md5 == null ? 0 : getLength(md5, songNum);
	}

	/**
	 * Get tune path contained in the commented line above the song length line.
	 * <BR>
	 * e.g. "; <B>/DEMOS/0-9/2_Hours_NOT_Enough.sid</B>" followed by<BR>
	 * "539be0485ad1fb958770fb9f069ae8c8=0:59"
	 * 
	 * @param tune
	 *            MD5 checksum of the tune is used to identify the path
	 * @return path of the tune
	 */
	public String getPath(final SidTune tune) {
		final String md5 = tune.getMD5Digest(version);
		final String comment = md5 != null ? database.getPropertyString("Database", "_" + md5, null) : null;
		return comment != null ? comment.substring(1).trim() : "";
	}

	protected Random random = new Random();

	public String getRandomPath() {
		String[] sectionProperties = database.sectionProperties("Database");
		int rndIndex = (Math.abs(random.nextInt(Integer.MAX_VALUE)) % sectionProperties.length) & Integer.MAX_VALUE - 1;
		String md5Comment = sectionProperties[rndIndex];
		final String comment = md5Comment != null ? database.getPropertyString("Database", "_" + md5Comment, null)
				: null;
		return comment != null ? comment.substring(1).trim() : null;
	}

	/**
	 * Get song length of a specific song number contained in the song length line
	 * identified by MD5 checksum.<BR>
	 * e.g. "b67e3121c8d771f3f06020b47232fc80=0:24 0:28 <B>1:03</B> 0:22 1:22 0:22"
	 * for song number 3.
	 * 
	 * @param md5
	 *            MD5 checksum to get the song length
	 * @param songNum
	 *            song number
	 * @return song length of the song number
	 */
	private int getLength(final String md5, final int songNum) {
		final String times = database.getPropertyString("Database", md5, null);
		return times != null ? new TimeConverter().fromString(times.split(" ")[songNum - 1]) : 0;
	}

}
