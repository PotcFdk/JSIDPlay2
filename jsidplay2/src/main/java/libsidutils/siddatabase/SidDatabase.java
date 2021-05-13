package libsidutils.siddatabase;

import static libsidplay.sidtune.SidTune.RESET;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import libsidplay.sidtune.MD5Method;
import libsidplay.sidtune.SidTune;
import libsidutils.ZipFileUtils;
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

	private MD5Method version;
	private final IniReader database;
	private final TimeConverter timeConverter = new TimeConverter();

	public SidDatabase(final File hvscRoot) throws IOException {
		try (InputStream is = getInputStreamAndSetVersion(hvscRoot)) {
			database = new IniReader(is);
		}
	}

	private InputStream getInputStreamAndSetVersion(File hvscRoot) throws FileNotFoundException {
		File file = ZipFileUtils.newFile(hvscRoot, SONGLENGTHS_FILE_TXT);
		version = MD5Method.MD5_PSID_HEADER;
		File songLengthFileMd5 = ZipFileUtils.newFile(hvscRoot, SONGLENGTHS_FILE_MD5);
		if (songLengthFileMd5.exists() && songLengthFileMd5.canRead()) {
			file = songLengthFileMd5;
			version = MD5Method.MD5_CONTENTS;
		}
		return ZipFileUtils.newFileInputStream(file);
	}

	/**
	 * Get tune length (sum of all song length contained in the tune) in seconds.
	 *
	 * @param tune tune to get the length for
	 * @return tune length in seconds
	 */
	public double getTuneLength(final SidTune tune) {
		double length = 0;
		final String md5 = tune.getMD5Digest(version);
		for (int songNum = 1; songNum <= tune.getInfo().getSongs(); songNum++) {
			length += getLength(md5, songNum);
		}
		return length;
	}

	/**
	 * Get song length of the current song in seconds.
	 *
	 * @param tune tune to determine the current song
	 * @return song length of the current song of the tune
	 */
	public double getSongLength(final SidTune tune) {
		if (tune == RESET) {
			return 0;
		}
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
	 * @param tune MD5 checksum of the tune is used to identify the path
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
		int rndIndex = Math.abs(random.nextInt(Integer.MAX_VALUE)) % sectionProperties.length & Integer.MAX_VALUE - 1;
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
	 * @param md5     MD5 checksum to get the song length
	 * @param songNum song number
	 * @return song length of the song number
	 */
	private double getLength(final String md5, final int songNum) {
		final String times = database.getPropertyString("Database", md5, null);
		return times != null ? timeConverter.fromString(times.split(" ")[songNum - 1]) : 0;
	}

}
