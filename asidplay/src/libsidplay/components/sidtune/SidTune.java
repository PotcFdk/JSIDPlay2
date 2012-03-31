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
package libsidplay.components.sidtune;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author Ken Händel
 * 
 */
public abstract class SidTune {
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
	
	public enum Clock { UNKNOWN, PAL, NTSC, ANY }

	public enum Model { UNKNOWN, MOS6581, MOS8580, ANY }
	
	public enum Compatibility { C64, PSID, R64, BASIC }
	
	protected SidTuneInfo info = new SidTuneInfo();

	protected final Speed songSpeed[] = new Speed[SIDTUNE_MAX_SONGS];

	protected final Clock clockSpeed[] = new Clock[SIDTUNE_MAX_SONGS];

	/** Known SID names. MUS loader scans for these. */
	private static final String defaultMusNames[] = new String[] {
		".mus", ".MUS", ".str", ".STR"
	};
	
	protected SidTune() {
		Arrays.fill(songSpeed, info.songSpeed);
		Arrays.fill(clockSpeed, info.clockSpeed);
	}

	public static SidTune load(final String fileName) throws IOException, SidTuneError {
		// ancient .mus and whatnot support.
		final byte[] fileBuf1 = loadFile(fileName);
		if (fileBuf1 == null) {
			/* no file found? return error. */
			return null;
		}
		
		SidTune s = PSid.load(fileBuf1);
		if (s != null) {
			s.info.dataFileLen = fileBuf1.length;
			s.fillPathName(fileName);
			return s;
		}
		
		/* load MUS */
		s = Mus.load(fileBuf1, null);
		if (s != null) {
			s.info.dataFileLen = fileBuf1.length;
			s.fillPathName(fileName);

			byte[] fileBuf2 = null;

			/* Try to load via .MUS / .STR naming convention */
			for (final String extension : defaultMusNames) {
				final String fileName2 = fileName.replaceFirst("\\.\\w+$", extension);
				// Do not load the first file again if names are equal.
				if (fileName.equalsIgnoreCase(fileName2)) {
					continue;
				}
		
				try {
					fileBuf2 = loadFile(fileName2);
					if (fileBuf2 != null) {
						break;
					}
				}
				catch (final FileNotFoundException e) {
				}
			}

			// try to load a MUS stereo tune by _a.mus / _b.mus naming convention.
			if (fileBuf2 == null) {
				if (fileName.toLowerCase().endsWith("_a.mus")) {
					final String fileName2 = fileName.replace("_a.mus", "_b.mus").replace("_A.MUS", "_B.MUS");
					try {
						fileBuf2 = loadFile(fileName2);
					}
					catch (final FileNotFoundException e) {
					}
				} else if (fileName.toLowerCase().endsWith("_b.mus")) {
					final String fileName2 = fileName.replace("_b.mus", "_a.mus").replace("_B.MUS", "_A.MUS");
					try {
						fileBuf2 = loadFile(fileName2);
					}
					catch (final FileNotFoundException e) {
					}
				}
			}

			if (fileBuf2 != null) {
				s = Mus.load(fileBuf1, fileBuf2);
				if (s != null) {
					s.info.dataFileLen = fileBuf1.length;
					s.fillPathName(fileName);
				}
			}

			return s;
		}
		
		return null;
	}

	public static SidTune load(final InputStream stream) throws IOException, SidTuneError {
		// ancient .mus and whatnot support.
		final int maxLength = 65536;
		final byte[] fileBuf1 = new byte[65536];
		int count, len = 0;
		while (len < maxLength
				&& (count = stream.read(fileBuf1, len, maxLength - len)) >= 0) {
			len += count;
		}
		
		/* Avoid Arrays.copyOf(), not available on dalvik */
		byte[] buffer = new byte[len];
		for (int i = 0; i < len; i ++) {
			buffer[i] = fileBuf1[i];
		}
		
		SidTune s = PSid.load(buffer);
		if (s != null) {
			return s;
		}
		
		s = Mus.load(buffer, null);
		if (s != null) {
			return s;
		}
		
		s = Prg.load(null, buffer);
		if (s != null) {
			return s;
		}
		
		return null;
	}

	/**
	 * Select sub-song (0 = default starting song) and retrieve active song
	 * information.
	 * 
	 * @param songNum
	 * @return
	 */
	public final SidTuneInfo opGet(final int songNum) {
		selectSong(songNum);
		return info;
	}

	/**
	 * Select sub-song (0 = default starting song) and return active song number
	 * out of [1,2,..,SIDTUNE_MAX_SONGS].
	 * 
	 * @param selectedSong
	 * @return
	 */
	public int selectSong(final int selectedSong) {
		int song = selectedSong;
		// Determine and set starting song number.
		if (selectedSong == 0) {
			song = info.startSong;
		}
		if (selectedSong > info.songs || selectedSong > SIDTUNE_MAX_SONGS) {
			song = info.startSong;
		}
		info.currentSong = song;
		// Retrieve song speed definition.
		if (info.compatibility == Compatibility.R64) {
			info.songSpeed = Speed.CIA_1A;
		} else if (info.compatibility == Compatibility.PSID) {
			// This SPEED field. It would most likely break compatibility to lots of
			// sidtunes, which have been converted from .SID format and vice
			// versa.
			// The .SID format does the bit-wise/song-wise evaluation of the
			// SPEED value correctly, like it is described in the PlaySID
			// documentation.
			info.songSpeed = songSpeed[song - 1 & 31];
		} else {
			info.songSpeed = songSpeed[song - 1];
		}
		info.clockSpeed = clockSpeed[song - 1];
		return song;
	}

	/**
	 * Retrieve sub-song specific information. Beware! Still member-wise copy!
	 * 
	 * @return
	 */
	public final SidTuneInfo getInfo() {
		return info;
	}

	/**
	 * Whether sidtune uses two SID chips.
	 * 
	 * @return
	 */
	public boolean isStereo() {
		return info.sidChipBase2 != 0;
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
	 * Does not affect status of object, and therefore can be used to load
	 * files. Error string is put into info.statusString, though.
	 * 
	 * @param fileName
	 * @param bufferRef
	 * @return
	 * @throws FileNotFoundException
	 */
	private static byte[] loadFile(final String fileName) throws IOException {
		final InputStream myIn = new FileInputStream(fileName);
		final int length = Math.min(65536, (int) new File(fileName).length());
		final byte[] data = new byte[length];
		int count, pos = 0;
		while (pos < length
				&& (count = myIn.read(data, pos, length - pos)) >= 0) {
			pos += count;
		}
		myIn.close();
		return data;
	}

	/**
	 * Convert 32-bit PSID-style speed word to internal tables.
	 * 
	 * @param speed
	 * @param clock
	 */
	protected void convertOldStyleSpeedToTables(long speed, final Clock clock) {
		// Create the speed/clock setting tables.
		//
		// This routine implements the PSIDv2NG compliant speed conversion. All
		// tunes
		// above 32 use the same song speed as tune 32
		final int toDo = info.songs <= SIDTUNE_MAX_SONGS ? info.songs : SIDTUNE_MAX_SONGS;
		for (int s = 0; s < toDo; s++) {
			clockSpeed[s] = clock;
			if ((speed & (1 << s)) != 0) {
				songSpeed[s] = Speed.CIA_1A;
			} else {
				songSpeed[s] = Speed.VBI;
			}
		}
	}

	protected static String convertPetsciiToAscii(final byte[] petscii, final int startOffset) {
		String result = "";
		for (int idx = startOffset; idx < petscii.length; idx ++) {
			final short out = _sidtune_CHRtab[petscii[idx] & 0xff];
			result += (char) out;
		}
		return result;
	}

	/**
	 * Petscii to Ascii conversion table.<BR>
	 * 
	 * CHR$ conversion table (0x01 = no output)
	 */
	private static final short _sidtune_CHRtab[] = { 0x0, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x0d, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x20, 0x21, 0x1, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b, 0x24, 0x5d, 0x20, 0x20,
		/* alternative: CHR$(92=0x5c) => ISO Latin-1(0xa3) */
		0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, 0x7c, 0x5c, 0x5c, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x5c, 0x23, 0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, 0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26, 0x5c,
		/* 0x80-0xFF */
		0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c, 0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, 0x2f, 0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, 0x2d, 0x2d, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x23, 0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, 0x7c, 0x5c, 0x5c, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x5c, 0x23, 0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, 0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26, 0x5c, 0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c, 0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, 0x2f, 0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, 0x2d, 0x2d, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x23 };

	private void fillPathName(final String fileName) {
		info.filename = fileName;
	}

	public String getMD5Digest() {
		return null;
	}

}
