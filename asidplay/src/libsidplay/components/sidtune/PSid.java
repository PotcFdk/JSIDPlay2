/**
 *                             PlaySID one-file format support.
 *                             --------------------------------
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import libsidplay.Reloc65;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.mem.IPSIDDrv;

class PSid extends Prg {
	private static final String txt_badAddr = "SIDTUNE ERROR: Bad address data";

	private static final String txt_corrupt = "SIDTUNE ERROR: File is incomplete or corrupt";

	private static final String txt_badReloc = "SIDTUNE ERROR: Bad reloc data";

	private static final MessageDigest md5Digest;

	static {
		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Header has been extended for 'RSID' format<BR>
	 * 
	 * The following changes are present:
	 * <UL>
	 * <LI> id = 'RSID'
	 * <LI> version = 2 only
	 * <LI> play, load and speed reserved 0
	 * <LI> psid specific flag reserved 0
	 * <LI> init cannot be under ROMS/IO
	 * <LI> load cannot be less than 0x0801 (start of basic)
	 * </UL>
	 * all values big-endian
	 * 
	 * @author Ken Händel
	 * 
	 */
	private static class PHeader {
		protected static final int SIZE = 124;

		public PHeader(final byte[] s) {
			final ByteBuffer b = ByteBuffer.wrap(s);

			b.get(id);
			version = b.getShort();
			data = b.getShort();
			load = b.getShort();
			init = b.getShort();
			play = b.getShort();
			songs = b.getShort();
			start = b.getShort();
			speed = b.getInt();

			b.get(name);
			b.get(author);
			b.get(released);

			if (version >= 2) {
				flags = b.getShort();
				// XXX what does it mean when it says "2B" is the version?
				relocStartPage = b.get();
				relocPages = b.get();
				reserved = b.getShort();
			}
		}

		/**
		 *  Magic (PSID or RSID)
		 */
		public byte[] id = new byte[4];

		/**
		 * 0x0001 or 0x0002
		 */
		public short /* uint8_t */version;

		/**
		 * 16-bit offset to binary data in file
		 */
		public short /* uint8_t */data;

		/**
		 * 16-bit C64 address to load file to
		 */
		public short /* uint8_t */load;

		/**
		 * 16-bit C64 address of init subroutine
		 */
		public short /* uint8_t */init;

		/**
		 * 16-bit C64 address of play subroutine
		 */
		public short /* uint8_t */play;

		/**
		 * number of songs
		 */
		public short /* uint8_t */songs;

		/**
		 * start song out of [1..256]
		 */
		public short /* uint8_t */start;

		/**
		 * 32-bit speed info:<BR>
		 * 
		 * bit: 0=50 Hz, 1=CIA 1 Timer A (default: 60 Hz)
		 */
		public int /* uint8_t */speed;

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 */
		public byte name[] = new byte[32];

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 */
		public byte author[] = new byte[32];

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 */
		public byte released[] = new byte[32];

		/**
		 * only version 0x0002
		 */
		public short flags;

		/**
		 * only version 0x0002B
		 */
		public byte relocStartPage;

		/**
		 * only version 0x0002B
		 */
		public byte relocPages;

		/**
		 * only version 0x0002
		 */
		@SuppressWarnings("unused")
		public short reserved;
	}

	//
	// PSID_SPECIFIC and PSID_BASIC are mutually exclusive
	//

	public static final int PSID_MUS = 1 << 0;

	public static final int PSID_SPECIFIC = 1 << 1;

	public static final int PSID_BASIC = 1 << 1;

	public static final int PSID_CLOCK = 3 << 2;

	public static final int PSID_SIDMODEL = 3 << 4;

	//
	// These are also used in the emulator engine!
	//

	public static final int PSID_CLOCK_UNKNOWN = 0;

	public static final int PSID_CLOCK_PAL = 1 << 2;

	public static final int PSID_CLOCK_NTSC = 1 << 3;

	public static final int PSID_CLOCK_ANY = PSID_CLOCK_PAL | PSID_CLOCK_NTSC;

	//
	// SID model
	//

	public static final int PSID_SIDMODEL_UNKNOWN = 0;

	public static final int PSID_SIDMODEL_6581 = 1 << 4;

	public static final int PSID_SIDMODEL_8580 = 1 << 5;

	public static final int PSID_SIDMODEL_ANY = PSID_SIDMODEL_6581
	| PSID_SIDMODEL_8580;

	//
	// sidtune format errors
	//

	final static String _sidtune_format_psid = "PlaySID one-file format (PSID)";

	final static String _sidtune_format_rsid = "Real C64 one-file format (RSID)";

	final static String _sidtune_unknown_psid = "Unsupported PSID version";

	final static String _sidtune_unknown_rsid = "Unsupported RSID version";

	final static String _sidtune_truncated = "ERROR: File is most likely truncated";

	final static String _sidtune_invalid = "ERROR: File contains invalid data";

	final static int _sidtune_psid_maxStrLen = 31;

	private final Reloc65 relocator = new Reloc65();

	private ByteBuffer relocatedBuffer;

	protected PSid() {
	}

	/**
	 * Temporary hack till real bank switching code added
	 * 
	 * @param addr
	 * A 16-bit effective address
	 * @return A default bank-select value for $01.
	 */
	private byte iomap(final int addr) {
		switch (info.compatibility) {
		case R64:
		case BASIC:
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

	@Override
	public int placeProgramInMemory(final byte[] c64buf) {
		super.placeProgramInMemory(c64buf);
		if (info.compatibility != Compatibility.BASIC) {
			return psidDrvReloc(c64buf);
		} else {
			return -1;
		}
	}

	private int psidDrvReloc(final byte[] m_ram) {
		final byte[] reloc_driver = relocatedBuffer.array();
		final int reloc_driverPos = relocatedBuffer.position();

		/* XXX: setting these vectors seems a bit dangerous because we will
		 * still run for some time */
		m_ram[0x0314] = reloc_driver[reloc_driverPos + 2]; /* IRQ */
		m_ram[0x0315] = reloc_driver[reloc_driverPos + 2 + 1];
		if (info.compatibility != SidTune.Compatibility.R64) {
			m_ram[0x0316] = reloc_driver[reloc_driverPos + 2 + 2]; /* Break */
			m_ram[0x0317] = reloc_driver[reloc_driverPos + 2 + 3];
			m_ram[0x0318] = reloc_driver[reloc_driverPos + 2 + 4]; /* NMI */
			m_ram[0x0319] = reloc_driver[reloc_driverPos + 2 + 5];
		}

		int pos = info.determinedDriverAddr;

		/* Place driver into RAM */
		System.arraycopy(reloc_driver, reloc_driverPos + 10, m_ram, pos, info.determinedDriverLength);

		// Tell C64 about song
		m_ram[pos++] = (byte) (info.currentSong - 1 & 0xff);
		if (info.songSpeed == Speed.VBI) {
			m_ram[pos] = 0;
		} else {
			// SIDTUNE_SPEED_CIA_1A
			m_ram[pos] = 1;
		}

		pos++;
		m_ram[pos++] = (byte) (info.initAddr & 0xff);
		m_ram[pos++] = (byte) (info.initAddr >> 8);
		m_ram[pos++] = (byte) (info.playAddr & 0xff);
		m_ram[pos++] = (byte) (info.playAddr >> 8);

		final int powerOnDelay = (int) (0x100 + (System.currentTimeMillis() & 0x1ff));
		m_ram[pos++] = (byte) (powerOnDelay & 0xff);
		m_ram[pos++] = (byte) (powerOnDelay >> 8);
		m_ram[pos++] = iomap(info.initAddr);
		m_ram[pos++] = iomap(info.playAddr);
		m_ram[pos + 1] = m_ram[pos + 0] = m_ram[0x02a6]; // PAL/NTSC flag
		pos++;

		// Add the required tune speed
		switch (info.clockSpeed) {
		case PAL:
			m_ram[pos++] = 1;
			break;
		case NTSC:
			m_ram[pos++] = 0;
			break;
		default: // UNKNOWN or ANY
			pos++;
			break;
		}

		// Default processor register flags on calling init
		if (info.compatibility == SidTune.Compatibility.R64) {
			m_ram[pos++] = 0;
		} else {
			m_ram[pos++] = 1 << MOS6510.SR_INTERRUPT;
		}

		return reloc_driver[reloc_driverPos + 0] & 0xff | (reloc_driver[reloc_driverPos + 1] & 0xff) << 8;
	}

	/**
	 * Common address resolution procedure
	 * 
	 * @param c64data
	 * @return
	 * @throws SidTuneError
	 */
	private boolean resolveAddrs() throws SidTuneError {
		// Originally used as a first attempt at an RSID
		// style format. Now reserved for future use
		if (info.playAddr == 0xffff) {
			info.playAddr = 0;
		}

		// loadAddr = 0 means, the address is stored in front of the C64 data.
		if (info.loadAddr == 0) {
			if (info.c64dataLen < 2) {
				throw new SidTuneError(txt_corrupt);
			}
			info.loadAddr = (program[fileOffset] & 0xff) + ((program[fileOffset + 1] & 0xff) << 8);
			fileOffset += 2;
			info.c64dataLen -= 2;
		}

		if (info.compatibility == Compatibility.BASIC) {
			if (info.initAddr != 0) {
				throw new SidTuneError(txt_badAddr);
			}
		} else if (info.initAddr == 0) {
			info.initAddr = info.loadAddr;
		}
		return true;
	}

	/**
	 * Check for valid relocation information, and calculate place for driver.
	 * The driver is only 1 block long, and we currently make use of this knowledge.
	 */
	protected void findPlaceForDriver() throws SidTuneError {
		final short startlp = (short) (info.loadAddr >> 8);
		final short endlp = (short) (startlp + (info.c64dataLen - 1 >> 8));

		// Fix relocation information
		if (info.relocStartPage == 0xFF) {
			info.relocPages = 0;
		} else if (info.relocPages == 0) {
			info.relocStartPage = 0;
		} else {
			// Calculate start/end page
			final short startp = info.relocStartPage;
			final short endp = (short) (startp + info.relocPages - 1 & 0xff);
			if (endp < startp) {
				throw new SidTuneError(txt_badReloc);
			}

			if (startp <= startlp && endp >= startlp || startp <= endlp && endp >= endlp) {
				throw new SidTuneError(txt_badReloc);
			}

			// Check that the relocation information does not use the following
			// memory areas: 0x0000-0x03FF, 0xA000-0xBFFF and 0xD000-0xFFFF
			if (startp < 0x04 || 0xa0 <= startp && startp <= 0xbf || startp >= 0xd0 || 0xa0 <= endp && endp <= 0xbf || endp >= 0xd0) {
				throw new SidTuneError(txt_badReloc);
			}
		}

		info.determinedDriverAddr = info.relocStartPage << 8;
		if (info.determinedDriverAddr == 0) {
			final int driverLen = 1;
			outer:
				for (int i = 0xcf; i >= 4; i --) {
					for (int j = 0; j < driverLen; j ++) {
						if (i + j >= startlp && i + j <= endlp) {
							continue outer;
						}
						if (i + j >= 0xa0 && i + j <= 0xbf) {
							continue outer;
						}
					}

					info.determinedDriverAddr = i << 8;
					break;
				}
		}

		if (info.determinedDriverAddr == 0) {
			throw new SidTuneError("Can't relocate tune: no pages left to store driver.");
		}

		relocatedBuffer = relocator.reloc65(IPSIDDrv.PSIDDRV, IPSIDDrv.PSIDDRV.length, info.determinedDriverAddr - 10, new HashMap<String, Integer>());
		if (relocatedBuffer == null) {
			throw new SidTuneError("Failed to relocate driver.");
		}
		info.determinedDriverLength = relocatedBuffer.limit() - 10;
	}

	protected static final SidTune load(final byte[] dataBuf) throws SidTuneError {
		Compatibility compatibility = Compatibility.C64;

		// File format check
		if (dataBuf.length < PHeader.SIZE) {
			return null;
		}

		final PSid sidtune = new PSid();

		// Require minimum size to allow access to the first few bytes.
		// Require a valid ID and version number.
		final PHeader pHeader = new PHeader(dataBuf);

		if (makeString(pHeader.id).equals("PSID")) {
			switch (pHeader.version) {
			case 1:
				compatibility = Compatibility.PSID;
				break;
			case 2:
				break;
			default:
				throw new SidTuneError(_sidtune_unknown_psid);
			}
		} else if (makeString(pHeader.id).equals("RSID")) {
			if (pHeader.version != 2) {
				throw new SidTuneError(_sidtune_unknown_rsid);
			}
			compatibility = Compatibility.R64;
		} else {
			return null;
		}

		sidtune.fileOffset = pHeader.data;
		sidtune.info.c64dataLen = dataBuf.length - sidtune.fileOffset;
		sidtune.info.loadAddr = pHeader.load & 0xffff;
		sidtune.info.initAddr = pHeader.init & 0xffff;
		sidtune.info.playAddr = pHeader.play & 0xffff;
		sidtune.info.songs = pHeader.songs & 0xffff;
		sidtune.info.startSong = pHeader.start & 0xffff;
		sidtune.info.compatibility = compatibility;
		int speed = pHeader.speed;

		if (sidtune.info.songs > SIDTUNE_MAX_SONGS) {
			sidtune.info.songs = SIDTUNE_MAX_SONGS;
		}
		/* fix bad song info */
		if (sidtune.info.songs == 0) {
			sidtune.info.songs++;
		}
		if (sidtune.info.startSong > sidtune.info.songs) {
			sidtune.info.startSong = 1;
		} else if (sidtune.info.startSong == 0) {
			sidtune.info.startSong++;
		}

		sidtune.info.sidModel = SidTune.Model.UNKNOWN;
		sidtune.info.relocPages = 0;
		sidtune.info.relocStartPage = 0;
		
		boolean wrappingMusFile = false;
		int clock = 0;
		int model = 0;
		if (pHeader.version >= 2) {
			if ((pHeader.flags & PSID_MUS) != 0) {
				clock = 3;
				wrappingMusFile = true;
			}

			switch (compatibility) {
			case C64:
				if ((pHeader.flags & PSID_SPECIFIC) != 0) {
					sidtune.info.compatibility = Compatibility.PSID;
				}
				break;
			case R64:
				if ((pHeader.flags & PSID_BASIC) != 0) {
					sidtune.info.compatibility = Compatibility.BASIC;
				}
				break;
			default:
				break;
			}

			/* based on order of Clock enum: UNKNOWN, PAL, NTSC, ANY */
			if ((pHeader.flags & PSID_CLOCK_PAL) != 0) {
				clock |= 1;
			}
			if ((pHeader.flags & PSID_CLOCK_NTSC) != 0) {
				clock |= 2;
			}

			if ((pHeader.flags & PSID_SIDMODEL_6581) != 0) {
				model |= 1;
			}
			if ((pHeader.flags & PSID_SIDMODEL_8580) != 0) {
				model |= 2;
			}

			sidtune.info.relocStartPage = (short) (pHeader.relocStartPage & 0xff);
			sidtune.info.relocPages = (short) (pHeader.relocPages & 0xff);
		}

		sidtune.info.clockSpeed = SidTune.Clock.values()[clock];
		sidtune.info.sidModel = SidTune.Model.values()[model];

		// Check reserved fields to force real c64 compliance
		// as required by the RSID specification
		if (compatibility == Compatibility.R64) {
			if (sidtune.info.loadAddr != 0 || sidtune.info.playAddr != 0 || speed != 0) {
				throw new SidTuneError(_sidtune_invalid);
			}
			// Real C64 tunes appear as CIA
			speed = ~0;
		}
		// Create the speed/clock setting table.
		sidtune.convertOldStyleSpeedToTables(speed, sidtune.info.clockSpeed);

		// Copy info strings, so they will not get lost.
		sidtune.info.numberOfInfoStrings = 3;

		// Name
		int i;
		for (i = 0; i < pHeader.name.length; i++) {
			if (pHeader.name[i] == 0) {
				break;
			}
		}
		sidtune.info.infoString[0] = makeString(pHeader.name, 0, Math.min(i, _sidtune_psid_maxStrLen));

		// Author
		for (i = 0; i < pHeader.author.length; i++) {
			if (pHeader.author[i] == 0) {
				break;
			}
		}
		sidtune.info.infoString[1] = makeString(pHeader.author, 0, Math.min(i, _sidtune_psid_maxStrLen));

		// Released
		for (i = 0; i < pHeader.released.length; i++) {
			if (pHeader.released[i] == 0) {
				break;
			}
		}
		sidtune.info.infoString[2] = makeString(pHeader.released, 0, Math.min(i, _sidtune_psid_maxStrLen));

		// missing title, author,
		// release fields
		for (i = 0; i < 3; i++) {
			if (sidtune.info.infoString[i].length() == 0) {
				sidtune.info.infoString[i] = "<?>";
				sidtune.info.infoString[i] = sidtune.info.infoString[i];
			}
		}

		if (wrappingMusFile) {
			final Mus mus = new Mus();
			mus.info = sidtune.info;
			mus.fileOffset = sidtune.fileOffset;
			return Mus.loadWithProvidedMetadata(dataBuf, null, mus);
		}

		sidtune.program = dataBuf;
		sidtune.resolveAddrs();
		sidtune.findPlaceForDriver();

		return sidtune;
	}

	private static String makeString(byte[] bytes) {
		return makeString(bytes, 0, bytes.length);
	}
	
	private static String makeString(byte[] bytes, int start, int len) {
		try {
			return new String(bytes, start, len, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/**
	 * Calculate MD5 of the SID tune according to the header information.
	 * 
	 * @return MD5 checksum as hex string
	 */
	@Override
	public final String getMD5Digest() {
		// Include C64 data.
		final byte[] myMD5 = new byte[info.c64dataLen + 6 + info.songs + (info.clockSpeed == SidTune.Clock.NTSC ? 1 : 0)];
		System.arraycopy(program, fileOffset, myMD5, 0, info.c64dataLen);
		int i = info.c64dataLen;
		myMD5[i++] = (byte) (info.initAddr & 0xff);
		myMD5[i++] = (byte) (info.initAddr >> 8);
		myMD5[i++] = (byte) (info.playAddr & 0xff);
		myMD5[i++] = (byte) (info.playAddr >> 8);
		myMD5[i++] = (byte) (info.songs & 0xff);
		myMD5[i++] = (byte) (info.songs >> 8);
		{ // Include song speed for each song.
			final int currentSong = info.currentSong;
			for (int s = 1; s <= info.songs; s++) {
				selectSong(s);
				myMD5[i++] = (byte) info.songSpeed.speedValue();
			}
			// Restore old song
			selectSong(currentSong);
		}
		// Deal with PSID v2NG clock speed flags: Let only NTSC
		// clock speed change the MD5 fingerprint. That way the
		// fingerprint of a PAL-speed sidtune in PSID v1, v2, and
		// PSID v2NG format is the same.
		if (info.clockSpeed == SidTune.Clock.NTSC) {
			myMD5[i++] = (byte) info.clockSpeed.ordinal();
			// NB! If the fingerprint is used as an index into a
			// song-lengths database or cache, modify above code to
			// allow for PSID v2NG files which have clock speed set to
			// SIDTUNE_CLOCK_ANY. If the SID player program fully
			// supports the SIDTUNE_CLOCK_ANY setting, a sidtune could
			// either create two different fingerprints depending on
			// the clock speed chosen by the player, or there could be
			// two different values stored in the database/cache.
		}

		String md5 = "";
		final byte[] encryptMsg = md5Digest.digest(myMD5);
		for (final byte anEncryptMsg : encryptMsg) {
			md5 += String.format("%02x", anEncryptMsg & 0xff);
		}
		return md5;
	}
}
