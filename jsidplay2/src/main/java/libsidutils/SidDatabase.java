package libsidutils;

import java.io.IOException;
import java.io.InputStream;

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
	public static final String SONGLENGTHS_FILE = "DOCUMENTS/Songlengths.txt";

	private final IniReader database;

	public SidDatabase(final InputStream input) throws IOException {
		database = new IniReader(input);
	}

	/**
	 * Get tune length (sum of all song length contained in the tune) in
	 * seconds.
	 * 
	 * @param tune
	 *            tune to get the length for
	 * @return tune length in seconds
	 */
	public int getTuneLength(final SidTune tune) {
		int length = 0;
		final String md5 = tune.getMD5Digest();
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
		final String md5 = tune.getMD5Digest();
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
		final String md5 = tune.getMD5Digest();
		final String comment = md5 != null ? database.getPropertyString("Database", "_" + md5, null) : null;
		return comment != null ? comment.substring(1).trim() : "";
	}

	/**
	 * Get song length of a specific song number contained in the song length
	 * line identified by MD5 checksum.<BR>
	 * e.g.
	 * "b67e3121c8d771f3f06020b47232fc80=0:24 0:28 <B>1:03</B> 0:22 1:22 0:22"
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
		return times != null ? IniReader.parseTime(times.split(" ")[songNum - 1]) : 0;
	}

}
