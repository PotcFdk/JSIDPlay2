/**
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package libsidplay.sidtune;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javafx.scene.image.Image;
import de.schlichtherle.truezip.file.TFileInputStream;

/**
 * @author Ken Händel
 * 
 */
public abstract class SidTune {
	public static final SidTune RESET = null;

	private static boolean ZIP_SUPPORTED;
	static {
		try {
			Class.forName("de.schlichtherle.truezip.file.TFileInputStream");
			ZIP_SUPPORTED = true;
		} catch (ClassNotFoundException e) {
		}
	}
	/**
	 * Maximum possible file size of C64 programs to load.
	 */
	private static final int MAX_MEM_64K = 65536;

	/**
	 * Also PSID file format limit.
	 */
	protected static final int SIDTUNE_MAX_SONGS = 256;

	public enum Speed {
		VBI(0), CIA_1A(60);

		private int speed;

		private Speed(int speed) {
			this.speed = speed;
		}

		public int speedValue() {
			return speed;
		}
	}

	/**
	 * Possible clock speeds of a SidTune.
	 */
	public enum Clock {
		UNKNOWN, PAL, NTSC, ANY
	}

	/**
	 * Possible models the SidTunes were meant to play on.
	 */
	public enum Model {
		UNKNOWN, MOS6581, MOS8580, ANY
	}

	/**
	 * SID types the SidTune may be compatible with.
	 */
	public enum Compatibility {
		PSIDv3, PSIDv2, PSIDv1, RSID, RSID_BASIC
	}

	protected SidTuneInfo info = new SidTuneInfo();

	protected final Speed songSpeed[] = new Speed[SIDTUNE_MAX_SONGS];

	protected static String PSIDDRIVER_ASM = "/libsidplay/sidtune/psiddriver.asm";

	/**
	 * Constructor
	 */
	protected SidTune() {
		Arrays.fill(songSpeed, Speed.VBI);
	}

	public static void useDriver(String sidDriver) {
		if (sidDriver != null) {
			PSIDDRIVER_ASM = sidDriver;
		}
	}

	/**
	 * Loads a file into a SidTune. Support of a lot of tunes here.
	 * 
	 * @param file
	 *            The file to load.
	 * 
	 * @return A SidTune instance of the specified file to load.
	 * 
	 * @throws IOException
	 * @throws SidTuneError
	 */
	public static SidTune load(final File file) throws IOException,
			SidTuneError {
		try {
			return MP3Tune.load(file);
		} catch (SidTuneError e1) {
			byte[] fileBuffer = getFileContents(file);
			try {
				return loadCommon(file.getName(), fileBuffer);
			} catch (SidTuneError e2) {
				return Mus.load(file, fileBuffer);
			}
		}
	}

	/**
	 * Loads an InputStream into a SidTune. Note: file based tunes are not
	 * supported (MUS/STR files, MP3)
	 * 
	 * @param url
	 *            URL of the given stream
	 * @param stream
	 *            The InputStream to load.
	 * 
	 * @return A SidTune of the specified InputStream.
	 * 
	 * @throws IOException
	 *             If the stream cannot be read.
	 * @throws SidTuneError
	 */
	public static SidTune load(String url, final InputStream stream)
			throws IOException, SidTuneError {
		return loadCommon(url, getFileContents(stream));
	}

	/**
	 * Load tune. Try several common SID tune formats to load.
	 * 
	 * @param name
	 *            name of the file (e.g. to check extension)
	 * @param fileBuffer
	 *            The tune data to load.
	 * 
	 * @return A SidTune of the specified contents.
	 * 
	 * @throws IOException
	 *             If the stream cannot be read.
	 * @throws SidTuneError
	 */
	private static SidTune loadCommon(String name, byte[] fileBuffer)
			throws SidTuneError {
		try {
			return PSid.load(name, fileBuffer);
		} catch (SidTuneError e1) {
			try {
				return Prg.load(name, fileBuffer);
			} catch (SidTuneError e2) {
				try {
					return P00.load(name, fileBuffer);
				} catch (SidTuneError e3) {
					return T64.load(name, fileBuffer);
				}
			}
		}
	}

	/**
	 * Select sub-song number (null = default starting song).
	 * 
	 * @param song
	 *            The chosen song.
	 */
	public final void setSelectedSong(final Integer song) {
		info.currentSong = song == null || song > info.songs ? info.startSong
				: song;
	}

	/**
	 * @return The active sub-song number
	 */
	public int getSelectedSong() {
		return info.currentSong == 0 || info.currentSong > info.songs ? info.startSong
				: info.currentSong;
	}

	/**
	 * Retrieve sub-song specific information. Beware! Still member-wise copy!
	 * 
	 * @return Sub-song specific information about the currently loaded tune.
	 */
	public final SidTuneInfo getInfo() {
		return info;
	}

	/**
	 * Does not affect status of object, and therefore can be used to load
	 * files.
	 * 
	 * @param file
	 *            The file to load.
	 * 
	 * @return The data of the loaded file.
	 * 
	 * @throws IOException
	 *             if the file could not be found.
	 */
	protected static final byte[] getFileContents(final File file)
			throws IOException {
		try (InputStream is = ZIP_SUPPORTED ? new TFileInputStream(file)
				: new FileInputStream(file)) {
			return getFileContents(is);
		}
	}

	private static byte[] getFileContents(final InputStream stream)
			throws IOException {
		final byte[] fileBuf = new byte[MAX_MEM_64K];
		int count, len = 0;
		while (len < MAX_MEM_64K
				&& (count = stream.read(fileBuf, len, MAX_MEM_64K - len)) >= 0) {
			len += count;
		}
		return Arrays.copyOf(fileBuf, len);
	}

	/**
	 * Convert 32-bit PSID-style speed word to internal tables.
	 * 
	 * @param speed
	 *            The speed to convert.
	 */
	protected final void convertOldStyleSpeedToTables(long speed) {
		for (int s = 0; s < SIDTUNE_MAX_SONGS; s++) {
			int i = s > 31 ? 31 : s;
			if ((speed & (1 << i)) != 0) {
				songSpeed[s] = Speed.CIA_1A;
			} else {
				songSpeed[s] = Speed.VBI;
			}
		}
	}

	/**
	 * Converts Petscii to Ascii.
	 * 
	 * @param petscii
	 *            The Petscii encoded data.
	 * @param startOffset
	 *            The offset to begin converting the Petscii data to Ascii.
	 * 
	 * @return The Petscii data converted to ASCII.
	 */
	protected static final String convertPetsciiToAscii(final byte[] petscii,
			final int startOffset) {
		StringBuilder result = new StringBuilder();
		for (int idx = startOffset; idx < petscii.length; idx++) {
			final short out = SIDTUNE_CHRTAB[petscii[idx] & 0xff];
			result.append((char) out);
		}
		return result.toString();
	}

	/**
	 * Petscii to Ascii conversion table.<BR>
	 * 
	 * CHR$ conversion table (0x01 = no output)
	 */
	private static final short SIDTUNE_CHRTAB[] = { 0x0, 0x1, 0x1, 0x1, 0x1,
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x0d, 0x1, 0x1, 0x1, 0x1,
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1,
			0x1, 0x20, 0x21, 0x1, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29,
			0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33, 0x34,
			0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f,
			0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a,
			0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55,
			0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b, 0x24, 0x5d, 0x20, 0x20,
			/* alternative: CHR$(92=0x5c) => ISO Latin-1(0xa3) */
			0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, 0x7c, 0x5c, 0x5c,
			0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x5c, 0x23, 0x5f, 0x23, 0x7c, 0x2f,
			0x58, 0x4f, 0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26, 0x5c,
			/* 0x80-0xFF */
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1,
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1,
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c,
			0x23, 0x7c, 0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, 0x2f,
			0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, 0x2d, 0x2d, 0x2f, 0x5c,
			0x5c, 0x2f, 0x2f, 0x23, 0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d,
			0x7c, 0x7c, 0x5c, 0x5c, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x5c, 0x23,
			0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, 0x23, 0x7c, 0x23, 0x2b, 0x7c,
			0x7c, 0x26, 0x5c, 0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c,
			0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, 0x2f, 0x2d, 0x2d,
			0x7c, 0x7c, 0x7c, 0x7c, 0x2d, 0x2d, 0x2d, 0x2f, 0x5c, 0x5c, 0x2f,
			0x2f, 0x23 };

	public final int getSongSpeedArray() {
		int speed = 0;
		for (int i = 0; i < 32; ++i) {
			if (songSpeed[i] != SidTune.Speed.VBI) {
				speed |= 1 << i;
			}
		}
		return speed;
	}

	/**
	 * Gets the speed of the selected song.
	 * 
	 * @param selected
	 *            The song to get the speed of.
	 * 
	 * @return The speed of the selected song.
	 */
	public final Speed getSongSpeed(int selected) {
		return songSpeed[selected - 1];
	}

	/**
	 * Copy program into C64 memory.
	 * 
	 * @param c64buf
	 * @return
	 * @throws SidTuneError
	 */
	public abstract int placeProgramInMemory(final byte[] c64buf);

	/**
	 * @param destFileName
	 *            Destination for the file.
	 * @param overWriteFlag
	 *            true = Overwrite existing file, false = Default<BR>
	 *            One could imagine an "Are you sure ?"-checkbox before
	 *            overwriting any file.
	 * @throws IOException
	 */
	public abstract void save(final String destFileName,
			final boolean overWriteFlag) throws IOException;

	/**
	 * Identify the player ID of a tune
	 * 
	 * @return the player IDs as a list
	 */
	public abstract Collection<String> identify();

	/**
	 * Return delay in C64 clocks before song init is done.
	 */
	public abstract long getInitDelay();

	/**
	 * A picture representing the tune (composer photo or cover art).
	 */
	public abstract Image getImage();

	/**
	 * MD5 for song length detection.
	 */
	public abstract String getMD5Digest();

}
