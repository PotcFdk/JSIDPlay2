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

import static libsidplay.common.SIDChip.DEF_BASE_ADDRESS;
import static libsidplay.sidtune.SidTune.Speed.CIA_1A;

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
		UNKNOWN(null), PAL(CPUClock.PAL), NTSC(CPUClock.NTSC), ANY(null);

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
		UNKNOWN(null), MOS6581(ChipModel.MOS6581), MOS8580(ChipModel.MOS8580), ANY(null);

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

	/**
	 * Loads a file as a SidTune (PSID, PRG, P00, T64, MUS, MP3).
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
		} catch (SidTuneError mp3Error) {
			byte[] fileContents = getContents(file);
			try {
				return load(file.getName(), fileContents);
			} catch (SidTuneError commonError) {
				try {
					return Mus.load(file, fileContents);
				} catch (SidTuneError musError) {
					throw (SidTuneError) mp3Error.initCause(musError.initCause(commonError));
				}
			}
		}
	}

	/**
	 * Loads an InputStream as a SidTune (PSID, PRG, P00, T64).
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
		return load(url, getContents(stream));
	}

	/**
	 * Load tune (PSID, PRG, P00, T64).
	 * 
	 * @param name
	 *            name of the file (for file extension check)
	 * @param fileContents
	 *            The tune data to load.
	 * 
	 * @return A SidTune of the specified contents.
	 * 
	 * @throws IOException
	 *             If the stream cannot be read.
	 * @throws SidTuneError
	 */
	protected static SidTune load(String name, byte[] fileContents) throws SidTuneError {
		try {
			return PSid.load(name, fileContents);
		} catch (SidTuneError psidError) {
			try {
				return Prg.load(name, fileContents);
			} catch (SidTuneError prgError) {
				try {
					return P00.load(name, fileContents);
				} catch (SidTuneError p00Error) {
					try {
						return T64.load(name, fileContents);
					} catch (SidTuneError t64Error) {
						throw (SidTuneError) t64Error.initCause(p00Error.initCause(prgError.initCause(psidError)));
					}
				}
			}
		}
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
	protected static final byte[] getContents(final File file) throws IOException {
		try (InputStream is = TFILE_IS != null ? (InputStream) TFILE_IS.newInstance(file) : new FileInputStream(file)) {
			return getContents(is);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IOException(file.getAbsolutePath(), e);
		}
	}

	private static byte[] getContents(final InputStream stream) throws IOException {
		final byte[] fileBuf = new byte[MAX_MEM_64K];
		int count, len = 0;
		while (len < MAX_MEM_64K && (count = stream.read(fileBuf, len, MAX_MEM_64K - len)) >= 0) {
			len += count;
		}
		return Arrays.copyOf(fileBuf, len);
	}

	protected SidTuneInfo info = new SidTuneInfo();

	/**
	 * Retrieve sub-song specific information.
	 * 
	 * @return Sub-song specific information about the currently loaded tune.
	 */
	public final SidTuneInfo getInfo() {
		return info;
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
		return CIA_1A;
	}

	/**
	 * Create 32-bit PSID-style speed word.
	 * 
	 * Each bit in 'speed' specifies the speed for the corresponding song
	 * number, i.e. bit 0 specifies the speed for song 1. If there are more than
	 * 32 song, the speed specified for song 32 is also used for all higher
	 * numbered songs.
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
	 * Detect fake-stereo SID (second SID at the same address).
	 * 
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * @return fake-stereo SID has been detected
	 */
	public static boolean isFakeStereoSid(IEmulationSection emulation, SidTune tune, int sidNum) {
		return sidNum > 0 && getSIDAddress(emulation, tune, sidNum - 1) == getSIDAddress(emulation, tune, sidNum);
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
	 * <LI>fake stereo - a second SID at the same address (0xd400)
	 * <LI>forced SID base - configured value for forced stereo or 3-SID output
	 * <LI>tune SID base - SID base detected by tune information
	 * <LI>0 - SID is not used
	 * </OL>
	 * Note: this function is static, even if no tune is loaded stereo mode can
	 * be configured!
	 */
	public static int getSIDAddress(IEmulationSection emulation, SidTune tune, int sidNum) {
		boolean forcedStereoTune;
		int forcedSidBase, tuneChipBase;
		switch (sidNum) {
		case 0:
			return DEF_BASE_ADDRESS;
		case 1:
			forcedStereoTune = emulation.isForceStereoTune();
			forcedSidBase = emulation.getDualSidBase();
			tuneChipBase = tune != RESET ? tune.getInfo().getSIDChipBase(sidNum) : 0;
			if (tuneChipBase == 0 && !forcedStereoTune && emulation.isFakeStereo()) {
				// A mono tune, not already forced to play in stereo mode shall
				// be played in fake stereo mode (2nd SID at the same base
				// address)
				return DEF_BASE_ADDRESS;
			}
			break;
		case 2:
			forcedStereoTune = emulation.isForce3SIDTune();
			forcedSidBase = emulation.getThirdSIDBase();
			tuneChipBase = tune != RESET ? tune.getInfo().getSIDChipBase(sidNum) : 0;
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
	 * @return play driver address
	 * @throws SidTuneError
	 */
	public abstract Integer placeProgramInMemory(final byte[] c64buf);

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

	public static boolean isSolelyPrg(SidTune tune) {
		return tune!=RESET && tune.getClass().equals(Prg.class);
	}
}
