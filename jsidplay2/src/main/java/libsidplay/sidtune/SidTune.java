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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.config.IEmulationSection;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;

/**
 * @author Ken Händel
 * 
 */
public abstract class SidTune {
	/**
	 * Do not load a tune, just reset C64.
	 */
	public static final SidTune RESET = null;
	/**
	 * Delay in cycles to wait for completion of a normal RESET.
	 */
	protected static final int RESET_INIT_DELAY = 2500000;

	private static Constructor<?> TFILE_IS = null;

	static {
		// support for files contained in a ZIP (optionally in the classpath)
		try {
			TFILE_IS = (Constructor<?>) Class.forName("de.schlichtherle.truezip.file.TFileInputStream")
					.getConstructor(File.class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
		}
	}

	/**
	 * Maximum possible file size of C64 programs to load.
	 */
	private static final int MAX_MEM_64K = 65536;

	public enum Speed {
		/**
		 * vertical blank interrupt (50Hz PAL, 60Hz NTSC)
		 */
		VBI(0),
		/**
		 * CIA 1 timer interrupt (default 60Hz)
		 */
		CIA_1A(60);

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
		UNKNOWN(CPUClock.PAL), PAL(CPUClock.PAL), NTSC(CPUClock.NTSC), ANY(CPUClock.PAL);

		private CPUClock cpuClock;

		private Clock(final CPUClock cpuClock) {
			this.cpuClock = cpuClock;
		}

		public CPUClock asCPUClock() {
			return cpuClock;
		}
	}

	/**
	 * Possible models the SidTunes were meant to play on.
	 */
	public enum Model {
		UNKNOWN(ChipModel.MOS6581), MOS6581(ChipModel.MOS6581), MOS8580(ChipModel.MOS8580), ANY(ChipModel.MOS6581);

		private ChipModel chipModel;

		private Model(final ChipModel chipModel) {
			this.chipModel = chipModel;
		}

		public ChipModel asChipModel() {
			return chipModel;
		}
	}

	/**
	 * SID types the SidTune may be compatible with.
	 */
	public enum Compatibility {
		PSIDv1, PSIDv2, PSIDv3, PSIDv4, RSID_BASIC, RSIDv2, RSIDv3
	}

	protected SidTuneInfo info = new SidTuneInfo();

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
	public static SidTune load(final File file) throws IOException, SidTuneError {
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
	public static SidTune load(String url, final InputStream stream) throws IOException, SidTuneError {
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
	private static SidTune loadCommon(String name, byte[] fileBuffer) throws SidTuneError {
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
		info.currentSong = song == null || song > info.songs ? info.startSong : song;
	}

	/**
	 * @return The active sub-song number
	 */
	public int getSelectedSong() {
		return info.currentSong == 0 || info.currentSong > info.songs ? info.startSong : info.currentSong;
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
	protected static final byte[] getFileContents(final File file) throws IOException {
		try (InputStream is = TFILE_IS != null ? (InputStream) TFILE_IS.newInstance(file) : new FileInputStream(file)) {
			return getFileContents(is);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IOException(file.getAbsolutePath());
		}
	}

	private static byte[] getFileContents(final InputStream stream) throws IOException {
		final byte[] fileBuf = new byte[MAX_MEM_64K];
		int count, len = 0;
		while (len < MAX_MEM_64K && (count = stream.read(fileBuf, len, MAX_MEM_64K - len)) >= 0) {
			len += count;
		}
		return Arrays.copyOf(fileBuf, len);
	}

	/**
	 * Temporary hack till real bank switching code added
	 * 
	 * @param addr
	 *            A 16-bit effective address
	 * @return A default bank-select value for $01.
	 */
	public int iomap(final int addr) {
		switch (info.compatibility) {
		case RSIDv2:
		case RSIDv3:
		case RSID_BASIC:
			return 0; // Special case, converted to 0x37 later
		default:
			if (addr == 0) {
				return 0; // Special case, converted to 0x37 later
			}
			if (addr < 0xa000) {
				return 0x37; // Basic-ROM, Kernal-ROM, I/O
			}
			if (addr < 0xd000) {
				return 0x36; // Kernal-ROM, I/O
			}
			if (addr >= 0xe000) {
				return 0x35; // I/O only
			}
			return 0x34; // RAM only (special I/O in PlaySID mode)
		}
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
		return Speed.CIA_1A;
	}

	/**
	 * Create 32-bit PSID-style speed word.
	 * 
	 * Each bit in 'speed' specifies the speed for the corresponding tune
	 * number, i.e. bit 0 specifies the speed for tune 1. If there are more than
	 * 32 tunes, the speed specified for tune 32 is also used for all higher
	 * numbered tunes.
	 * 
	 * A 0 bit specifies vertical blank interrupt (50Hz PAL, 60Hz NTSC), and a 1
	 * bit specifies CIA 1 timer interrupt (default 60Hz).
	 * 
	 * @return 32-bit PSID-style speed word (defaults to CIA 1 timer interrupt)
	 */
	public int getSongSpeedWord() {
		return ~0;
	}

	/**
	 * Is specified SID number in use?
	 * <OL>
	 * <LI>0 - first SID is always used
	 * <LI>1 - second SID is only used for stereo tunes
	 * <LI>2 - third SID is used for 3-SID tunes
	 * </OL>
	 */
	public static boolean isSIDUsed(IEmulationSection emulation, SidTune tune, int sidNum) {
		return getSIDAddress(emulation, tune, sidNum) != 0;
	}

	/**
	 * Get SID address of specified SID number
	 * <OL>
	 * <LI>0xd400 - always used for first SID
	 * <LI>forced SID base - configured value for forced stereo or 3-SID output
	 * <LI>tune SID base - SID base detected by tune information
	 * <LI>0 - SID is not used
	 * </OL>
	 * Note: this function is static, even if no tune is loaded stereo mode can
	 * be configured!
	 */
	public static int getSIDAddress(IEmulationSection emulation, SidTune tune, int sidNum) {
		boolean forcedStereoTune;
		int forcedSidBase;
		int tuneChipBase;
		switch (sidNum) {
		case 0:
			return 0xd400;
		case 1:
			forcedStereoTune = emulation.isForceStereoTune();
			forcedSidBase = emulation.getDualSidBase();
			tuneChipBase = tune != RESET ? tune.getInfo().getSidChipBase(sidNum) : 0;
			break;
		case 2:
			forcedStereoTune = emulation.isForce3SIDTune();
			forcedSidBase = emulation.getThirdSIDBase();
			tuneChipBase = tune != RESET ? tune.getInfo().getSidChipBase(sidNum) : 0;
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		if (forcedStereoTune) {
			return forcedSidBase;
		} else {
			return tuneChipBase;
		}
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
	 * @throws IOException
	 */
	public abstract void save(final String destFileName) throws IOException;

	/**
	 * Identify the player ID of a tune
	 * 
	 * @return the player IDs as a list
	 */
	public abstract Collection<String> identify();

	/**
	 * Search player ID Info.
	 * 
	 * @param playerName
	 *            player to get infos for
	 * @return player infos (or null, if not found)
	 */
	public abstract PlayerInfoSection getPlayerInfo(String playerName);

	/**
	 * MD5 for song length detection.
	 */
	public abstract String getMD5Digest();

	/**
	 * Return delay in C64 clocks before song init is done.
	 */
	protected abstract long getInitDelay();

	public static long getInitDelay(SidTune tune) {
		return tune != RESET ? tune.getInitDelay() : RESET_INIT_DELAY;
	}
}
