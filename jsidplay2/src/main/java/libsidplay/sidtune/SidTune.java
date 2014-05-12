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
import java.util.ArrayList;
import java.util.Arrays;

import javafx.scene.image.Image;
import de.schlichtherle.truezip.file.TFileInputStream;

/**
 * @author Ken Händel
 * 
 */
public abstract class SidTune {
	private static final int MAX_MEM_64K = 65536;

	/**
	 * Also PSID file format limit.
	 */
	protected static final int SIDTUNE_MAX_SONGS = 256;

	protected static final int SIDTUNE_MAX_CREDIT_STRINGS = 10;

	public enum Speed {
		VBI(0), CIA_1A(60);

		private int val;

		Speed(int n) {
			this.val = n;
		}

		public int speedValue() {
			return val;
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

	private String outputFilename;

	/**
	 * Constructor
	 */
	protected SidTune() {
		Arrays.fill(songSpeed, Speed.VBI);
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
		SidTune tune = null;
		try {
			tune = MP3Tune.load(file.getName(), file);
			if (tune != null) {
				return tune;
			}
			byte[] fileBuffer = getFileContents(file);
			tune = PSid.load(file.getName(), fileBuffer);
			if (tune != null) {
				return tune;
			}
			tune = Prg.load(file.getName(), fileBuffer);
			if (tune != null) {
				return tune;
			}
			tune = P00.load(file.getName(), fileBuffer);
			if (tune != null) {
				return tune;
			}
			tune = T64.load(file.getName(), fileBuffer);
			if (tune != null) {
				return tune;
			}
			tune = Mus.load(file, fileBuffer);
			if (tune != null) {
				return tune;
			}
			return null;
		} finally {
			if (tune != null) {
				tune.info.file = file;
			}
		}
	}

	/**
	 * Loads an InputStream into a SidTune.
	 * Note: MUS/STR files are not supported (they require a stereo file)
	 * 
	 * @param stream
	 *            The InputStream to load.
	 * @param url
	 *            URL of the given stream
	 * 
	 * @return A SidTune of the specified InputStream.
	 * 
	 * @throws IOException
	 *             If the stream cannot be read.
	 * @throws SidTuneError
	 */
	public static SidTune load(final InputStream stream, String url)
			throws IOException, SidTuneError {
		SidTune tune = null;
		byte[] fileBuffer = getFileContents(stream);
		tune = PSid.load(url, fileBuffer);
		if (tune != null) {
			return tune;
		}
		tune = Prg.load(url, fileBuffer);
		if (tune != null) {
			return tune;
		}
		tune = P00.load(url, fileBuffer);
		if (tune != null) {
			return tune;
		}
		tune = T64.load(url, fileBuffer);
		if (tune != null) {
			return tune;
		}
		return tune;
	}

	/**
	 * Select sub-song (0 = default starting song) and return active song number
	 * out of [1,2,..,SIDTUNE_MAX_SONGS].
	 * 
	 * @param selectedSong
	 *            The chosen song.
	 * 
	 * @return The active song number.
	 */
	public int selectSong(final int selectedSong) {
		int song = selectedSong;
		if (selectedSong == 0 || selectedSong > info.songs) {
			song = info.startSong;
		}
		info.currentSong = song;
		return song;
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
	public abstract ArrayList<String> identify();

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
	protected static byte[] getFileContents(final File file) throws IOException {
		try {
			Class.forName("de.schlichtherle.truezip.file.TFileInputStream");
			try (InputStream is = new TFileInputStream(file)) {
				return getFileContents(is);
			}
		} catch (ClassNotFoundException e) {
			// skip ZIP support, if not available!
			try (InputStream is = new FileInputStream(file)) {
				return getFileContents(is);
			}
		}
	}

	private static byte[] getFileContents(final InputStream stream)
			throws IOException {
		final byte[] fileBuf = new byte[MAX_MEM_64K];
		int count, len = 0;
		final int maxLength = fileBuf.length;
		while (len < maxLength
				&& (count = stream.read(fileBuf, len, maxLength - len)) >= 0) {
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
	protected void convertOldStyleSpeedToTables(long speed) {
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
	protected static String convertPetsciiToAscii(final byte[] petscii,
			final int startOffset) {
		StringBuilder result = new StringBuilder();
		for (int idx = startOffset; idx < petscii.length; idx++) {
			final short out = _sidtune_CHRtab[petscii[idx] & 0xff];
			result.append((char) out);
		}
		return result.toString();
	}

	/**
	 * Petscii to Ascii conversion table.<BR>
	 * 
	 * CHR$ conversion table (0x01 = no output)
	 */
	private static final short _sidtune_CHRtab[] = { 0x0, 0x1, 0x1, 0x1, 0x1,
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

	public String getMD5Digest() {
		return null;
	}

	public int getSongSpeedArray() {
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
	public Speed getSongSpeed(int selected) {
		return songSpeed[selected - 1];
	}

	/**
	 * Return delay in C64 clocks before song init is done.
	 */
	public abstract long getInitDelay();

	public Image getImage() {
		return null;
	}

	public void setOutputFilename(String outputFilename) {
		this.outputFilename = outputFilename;
	}

	public String getOutputFilename() {
		return outputFilename;
	}

}
