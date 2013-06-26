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
package libsidplay.sidtune;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javafx.scene.image.Image;

import libsidplay.Reloc65;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.mem.IPSIDDrv;

class PSid extends Prg {
	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties sidAuthors = new Properties();

	static {
		InputStream is = SidTune.class
				.getResourceAsStream("pictures.properties");
		try {
			sidAuthors.load(is);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	}

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
	 * <LI>id = 'RSID'
	 * <LI>version = 2 or 3 only
	 * <LI>play, load and speed reserved 0
	 * <LI>psid specific flag reserved 0
	 * <LI>init cannot be under ROMS/IO
	 * <LI>load cannot be less than 0x0801 (start of basic)
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
				/* 2B */
				relocStartPage = b.get();
				relocPages = b.get();
				/* 2SID */
				sidChip2MiddleNybbles = b.get();
				reserved = b.get();
			}
		}

		public PHeader() {
		}

		/**
		 * Magic (PSID or RSID)
		 */
		public byte[] id = new byte[4];

		/**
		 * 0x0001, 0x0002 or 0x0003
		 */
		public short version;

		/**
		 * 16-bit offset to binary data in file
		 */
		public short data;

		/**
		 * 16-bit C64 address to load file to
		 */
		public short load;

		/**
		 * 16-bit C64 address of init subroutine
		 */
		public short init;

		/**
		 * 16-bit C64 address of play subroutine
		 */
		public short play;

		/**
		 * number of songs
		 */
		public short songs;

		/**
		 * start song out of [1..256]
		 */
		public short start;

		/**
		 * 32-bit speed info:<BR>
		 * 
		 * bit: 0=50 Hz, 1=CIA 1 Timer A (default: 60 Hz)
		 */
		public int speed;

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 * For version 0x0003, all 32 chars can be used without zero
		 * termination. if less than 32 chars are used then it should be
		 * terminated with a trailing zero
		 */
		public byte name[] = new byte[32];

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 * For version 0x0003, all 32 chars can be used without zero
		 * termination. if less than 32 chars are used then it should be
		 * terminated with a trailing zero
		 */
		public byte author[] = new byte[32];

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 * For version 0x0003, all 32 chars can be used without zero
		 * termination. if less than 32 chars are used then it should be
		 * terminated with a trailing zero
		 */
		public byte released[] = new byte[32];

		/**
		 * only version 0x0002+
		 */
		public short flags;

		/**
		 * only version 0x0002+
		 */
		public byte relocStartPage;

		/**
		 * only version 0x0002+
		 */
		public byte relocPages;

		/**
		 * only version 0x0002+ reserved for version 0x0002 used in version
		 * 0x0003 to indicate second SID chip address
		 */
		public byte sidChip2MiddleNybbles;

		/**
		 * only version 0x0002+
		 */
		public byte reserved;

		public byte[] getArray() {
			final ByteBuffer b = ByteBuffer.allocate(SIZE);
			b.put(id);
			b.putShort(version);
			b.putShort(data);
			b.putShort(load);
			b.putShort(init);
			b.putShort(play);
			b.putShort(songs);
			b.putShort(start);
			b.putInt(speed);
			b.put(name);
			b.put(author);
			b.put(released);
			if (version >= 2) {
				b.putShort(flags);
				b.put(relocStartPage);
				b.put(relocPages);
				b.put(sidChip2MiddleNybbles);
				b.put(reserved);
			}
			return b.array();
		}

	}

	//
	// PSID_SPECIFIC and PSID_BASIC are mutually exclusive
	//

	public static final int PSID_MUS = 1 << 0;

	public static final int PSID_SPECIFIC = 1 << 1;

	public static final int PSID_BASIC = 1 << 1;

	private final Reloc65 relocator = new Reloc65();

	private ByteBuffer relocatedBuffer;

	private final byte[] driver = new byte[IPSIDDrv.PSIDDRV.length];

	private Image image;

	protected PSid() {
		// Reloc65 modifies the driver code, for that reason a copy of
		// IPSIDDrv.PSIDDRV should used instead
		// Otherwise A parallel searchIndexerThread can happen to fail!
		System.arraycopy(IPSIDDrv.PSIDDRV, 0, driver, 0,
				IPSIDDrv.PSIDDRV.length);
	}

	/**
	 * Temporary hack till real bank switching code added
	 * 
	 * @param addr
	 *            A 16-bit effective address
	 * @return A default bank-select value for $01.
	 */
	private byte iomap(final int addr) {
		switch (info.compatibility) {
		case RSID:
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

	@Override
	public int placeProgramInMemory(final byte[] c64buf) {
		super.placeProgramInMemory(c64buf);
		if (info.compatibility != Compatibility.RSID_BASIC) {
			return psidDrvReloc(c64buf);
		} else {
			c64buf[0x30c] = (byte) (info.currentSong - 1);
			return -1;
		}
	}

	private int psidDrvReloc(final byte[] m_ram) {
		final byte[] reloc_driver = relocatedBuffer.array();
		final int reloc_driverPos = relocatedBuffer.position();

		if (!(info.playAddr == 0 && info.loadAddr == 0x200)) {
			/*
			 * XXX: setting these vectors seems a bit dangerous because we will
			 * still run for some time
			 */
			m_ram[0x0314] = reloc_driver[reloc_driverPos + 2]; /* IRQ */
			m_ram[0x0315] = reloc_driver[reloc_driverPos + 2 + 1];
			if (info.compatibility != SidTune.Compatibility.RSID) {
				m_ram[0x0316] = reloc_driver[reloc_driverPos + 2 + 2]; /* Break */
				m_ram[0x0317] = reloc_driver[reloc_driverPos + 2 + 3];
				m_ram[0x0318] = reloc_driver[reloc_driverPos + 2 + 4]; /* NMI */
				m_ram[0x0319] = reloc_driver[reloc_driverPos + 2 + 5];
			}
		}
		int pos = info.determinedDriverAddr;

		/* Place driver into RAM */
		System.arraycopy(reloc_driver, reloc_driverPos + 10, m_ram, pos,
				info.determinedDriverLength);

		// Tell C64 about song
		m_ram[pos++] = (byte) (info.currentSong - 1);
		if (songSpeed[info.currentSong - 1] == Speed.VBI) {
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
		if (info.compatibility == Compatibility.RSID) {
			m_ram[pos++] = 0;
		} else {
			m_ram[pos++] = 1 << MOS6510.SR_INTERRUPT;
		}

		return reloc_driver[reloc_driverPos + 0] & 0xff
				| (reloc_driver[reloc_driverPos + 1] & 0xff) << 8;
	}

	/**
	 * Common address resolution procedure
	 *
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
				throw new SidTuneError("Song is truncated");
			}
			info.loadAddr = (program[fileOffset] & 0xff)
					+ ((program[fileOffset + 1] & 0xff) << 8);
			fileOffset += 2;
			info.c64dataLen -= 2;
		}

		if (info.compatibility == Compatibility.RSID_BASIC) {
			if (info.initAddr != 0) {
				throw new SidTuneError(
						"Init address given for a RSID tune with BASIC flag");
			}
		} else if (info.initAddr == 0) {
			info.initAddr = info.loadAddr;
		}
		return true;
	}

	/**
	 * Check for valid relocation information, and calculate place for driver.
	 * The driver is only 1 block long, and we currently make use of this
	 * knowledge.
	 */
	protected void findPlaceForDriver() throws SidTuneError {
		final short startlp = (short) (info.loadAddr >> 8);
		final short endlp = (short) (info.loadAddr + info.c64dataLen - 1 >> 8);

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
				throw new SidTuneError(
						String.format(
								"Relocation info is invalid: end before start: end=%02x, start=%02x",
								endp, startp));
			}

			if (startp <= startlp && endp >= startlp || startp <= endlp
					&& endp >= endlp) {
				throw new SidTuneError(
						String.format(
								"Relocation info is invalid: relocation in middle of song tune itself: songstart=%02x, songend=%02x, relocstart=%02x, relocend=%02x",
								startlp, endlp, startp, endp));
			}

			// Check that the relocation information does not use the following
			// memory areas: 0x0000-0x03FF, 0xA000-0xBFFF and 0xD000-0xFFFF
			if (startp < 0x04 || 0xa0 <= startp && startp <= 0xbf
					|| startp >= 0xd0 || 0xa0 <= endp && endp <= 0xbf
					|| endp >= 0xd0) {
				throw new SidTuneError(
						String.format(
								"Relocation info is invalid: beyond acceptable bounds (kernal, basic, io, < 4th page): %02x-%02x",
								startp, endp));
			}
		}

		info.determinedDriverAddr = info.relocStartPage << 8;
		if (info.determinedDriverAddr == 0) {
			final int driverLen = 1;
			outer: for (int i = 4; i < 0xd0; i++) {
				for (int j = 0; j < driverLen; j++) {
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
			throw new SidTuneError(
					"Can't relocate tune: no pages left to store driver.");
		}

		relocatedBuffer = relocator.reloc65(driver, driver.length,
				info.determinedDriverAddr - 10, new HashMap<String, Integer>());

		if (relocatedBuffer == null) {
			throw new SidTuneError("Failed to relocate driver.");
		}
		info.determinedDriverLength = relocatedBuffer.limit() - 10;
	}

	protected static final SidTune load(final byte[] dataBuf)
			throws SidTuneError {
		if (dataBuf.length < PHeader.SIZE) {
			return null;
		}
		final PHeader pHeader = new PHeader(dataBuf);

		final PSid sidtune = new PSid();
		sidtune.fileOffset = pHeader.data;
		sidtune.info.c64dataLen = dataBuf.length - sidtune.fileOffset;
		sidtune.info.loadAddr = pHeader.load & 0xffff;
		sidtune.info.initAddr = pHeader.init & 0xffff;
		sidtune.info.playAddr = pHeader.play & 0xffff;

		sidtune.info.songs = pHeader.songs & 0xffff;
		if (sidtune.info.songs == 0) {
			sidtune.info.songs++;
		}
		if (sidtune.info.songs > SIDTUNE_MAX_SONGS) {
			sidtune.info.songs = SIDTUNE_MAX_SONGS;
		}
		sidtune.info.startSong = pHeader.start & 0xffff;
		if (sidtune.info.startSong > sidtune.info.songs) {
			sidtune.info.startSong = 1;
		} else if (sidtune.info.startSong == 0) {
			sidtune.info.startSong++;
		}

		int speed = pHeader.speed;

		if (Arrays.equals(pHeader.id, new byte[] { 'P', 'S', 'I', 'D' })) {
			switch (pHeader.version) {
			case 1:
				sidtune.info.compatibility = Compatibility.PSIDv1;
				break;
			case 2:
				sidtune.info.compatibility = Compatibility.PSIDv2;
				if ((pHeader.flags & PSID_SPECIFIC) != 0) {
					throw new SidTuneError(
							"PSID-specific files are not supported by this player");
				}
				break;
			case 3:
				sidtune.info.compatibility = Compatibility.PSIDv3;
				break;
			default:
				throw new SidTuneError("PSID version must be 1, 2 or 3, now: "
						+ pHeader.version);
			}
		} else if (Arrays.equals(pHeader.id, new byte[] { 'R', 'S', 'I', 'D' })) {
			if ((pHeader.version < 2) || (pHeader.version > 3)) {
				throw new SidTuneError("RSID version must be 2 or 3, now: "
						+ pHeader.version);
			}
			sidtune.info.compatibility = (pHeader.flags & PSID_BASIC) != 0 ? Compatibility.RSID_BASIC
					: Compatibility.RSID;

			if (sidtune.info.loadAddr != 0 || sidtune.info.playAddr != 0
					|| speed != 0) {
				throw new SidTuneError(
						"RSID tune specified load, play or speed information.");
			}
			speed = ~0; /* CIA */
		} else {
			return null;
		}

		int clock = 0;
		int model1 = 0;
		int model2 = 0;
		if (pHeader.version >= 2) {
			clock = (pHeader.flags >> 2) & 3;
			model1 = (pHeader.flags >> 4) & 3;

			sidtune.info.relocStartPage = (short) (pHeader.relocStartPage & 0xff);
			sidtune.info.relocPages = (short) (pHeader.relocPages & 0xff);

			if (pHeader.version >= 3) {
				model2 = (pHeader.flags >> 6) & 3;

				/* Handle 2nd SID chip location */
				int sid2loc = 0xd000 | (pHeader.sidChip2MiddleNybbles & 0xff) << 4;
				if (((sid2loc >= 0xd420 && sid2loc < 0xd800) || sid2loc >= 0xde00)
						&& (sid2loc & 0x10) == 0) {
					sidtune.info.sidChipBase2 = sid2loc;
				}
			}
		}
		sidtune.info.clockSpeed = SidTune.Clock.values()[clock];
		sidtune.info.sid1Model = SidTune.Model.values()[model1];
		sidtune.info.sid2Model = SidTune.Model.values()[model2];

		// Create the speed/clock setting table.
		sidtune.convertOldStyleSpeedToTables(speed);

		// Copy info strings, so they will not get lost.
		sidtune.info.numberOfInfoStrings = 3;

		// Name
		int i;
		for (i = 0; i < pHeader.name.length; i++) {
			if (pHeader.name[i] == 0) {
				break;
			}
		}
		sidtune.info.infoString[0] = makeString(pHeader.name, 0,
				Math.min(i, pHeader.name.length));

		// Author
		for (i = 0; i < pHeader.author.length; i++) {
			if (pHeader.author[i] == 0) {
				break;
			}
		}
		sidtune.info.infoString[1] = makeString(pHeader.author, 0,
				Math.min(i, pHeader.author.length));

		// Released
		for (i = 0; i < pHeader.released.length; i++) {
			if (pHeader.released[i] == 0) {
				break;
			}
		}
		sidtune.info.infoString[2] = makeString(pHeader.released, 0,
				Math.min(i, pHeader.released.length));

		// missing title, author,
		// release fields
		for (i = 0; i < 3; i++) {
			if (sidtune.info.infoString[i].length() == 0) {
				sidtune.info.infoString[i] = "<?>";
				sidtune.info.infoString[i] = sidtune.info.infoString[i];
			}
		}

		String authorInfo = sidtune.info.infoString[1];
		String photoRes = sidAuthors.getProperty(authorInfo);

		if (photoRes != null) {
			photoRes = "Photos/" + photoRes;
			sidtune.image = new Image(SidTune.class.getResource(photoRes)
					.toString());
		}

		if ((pHeader.flags & PSID_MUS) != 0) {
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

	private static String makeString(byte[] bytes, int start, int len) {
		try {
			return new String(bytes, start, len, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	@Override
	public void save(final String name, final boolean overWrite)
			throws IOException {
		final FileOutputStream fos = new FileOutputStream(name, overWrite);

		final PHeader myHeader = new PHeader();
		myHeader.id = "PSID".getBytes();
		if (info.sidChipBase2 != 0) {
			myHeader.version = 3;
		} else {
			myHeader.version = 2;
		}
		myHeader.data = PHeader.SIZE;
		myHeader.songs = (short) info.songs;
		myHeader.start = (short) info.startSong;
		myHeader.speed = getSongSpeedArray();

		short tmpFlags = 0;
		myHeader.init = (short) info.initAddr;
		myHeader.relocStartPage = (byte) info.relocStartPage;
		myHeader.relocPages = (byte) info.relocPages;

		switch (info.compatibility) {
		case RSID_BASIC:
			tmpFlags |= PSID_BASIC;
			//$FALL-THROUGH$

		case RSID:
			myHeader.id = "RSID".getBytes();
			myHeader.speed = 0;
			break;

		case PSIDv1:
			tmpFlags |= PSID_SPECIFIC;
			//$FALL-THROUGH$

		default:
			myHeader.play = (short) info.playAddr;
			break;
		}

		// @FIXME@ Need better solution. Make it possible to override MUS
		// strings
		if (info.numberOfInfoStrings == 3) {
			if (info.infoString[0].length() == 32) {
				myHeader.version = 3;
			} else if (info.infoString[1].length() == 32) {
				myHeader.version = 3;
			} else if (info.infoString[2].length() == 32) {
				myHeader.version = 3;
			}

			for (int i = 0; i < info.infoString[0].length(); i++) {
				myHeader.name[i] = (byte) info.infoString[0].charAt(i); // ISO-8859-1
			}
			for (int i = 0; i < info.infoString[1].length(); i++) {
				myHeader.author[i] = (byte) info.infoString[1].charAt(i); // ISO-8859-1
			}
			for (int i = 0; i < info.infoString[2].length(); i++) {
				myHeader.released[i] = (byte) info.infoString[2].charAt(i); // ISO-8859-1
			}
		}

		tmpFlags |= info.clockSpeed.ordinal() << 2;
		tmpFlags |= info.sid1Model.ordinal() << 4;
		tmpFlags |= info.sid2Model.ordinal() << 6;
		myHeader.flags = tmpFlags;

		fos.write(myHeader.getArray());

		final byte saveAddr[] = new byte[2];
		saveAddr[0] = (byte) (info.loadAddr & 255);
		saveAddr[1] = (byte) (info.loadAddr >> 8);
		fos.write(saveAddr);

		// Data starts at: bufferaddr + fileoffset
		// Data length: datafilelen - fileoffset
		fos.write(program, fileOffset, info.dataFileLen - fileOffset);
		fos.close();
	}

	/**
	 * Calculate MD5 of the SID tune according to the header information.
	 * 
	 * @return MD5 checksum as hex string
	 */
	@Override
	public final String getMD5Digest() {
		// Include C64 data.
		final byte[] myMD5 = new byte[info.c64dataLen + 6 + info.songs
				+ (info.clockSpeed == SidTune.Clock.NTSC ? 1 : 0)];
		System.arraycopy(program, fileOffset, myMD5, 0, info.c64dataLen);
		int i = info.c64dataLen;
		myMD5[i++] = (byte) (info.initAddr & 0xff);
		myMD5[i++] = (byte) (info.initAddr >> 8);
		myMD5[i++] = (byte) (info.playAddr & 0xff);
		myMD5[i++] = (byte) (info.playAddr >> 8);
		myMD5[i++] = (byte) (info.songs & 0xff);
		myMD5[i++] = (byte) (info.songs >> 8);
		for (int s = 1; s <= info.songs; s++) {
			myMD5[i++] = (byte) songSpeed[s - 1].speedValue();
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

		StringBuilder md5 = new StringBuilder();
		final byte[] encryptMsg = md5Digest.digest(myMD5);
		for (final byte anEncryptMsg : encryptMsg) {
			md5.append(String.format("%02x", anEncryptMsg & 0xff));
		}
		return md5.toString();
	}

	@Override
	public long getInitDelay() {
		// Zero does not always work well (Synth_sample)!
		return 2500;
	}

	@Override
	public Image getImage() {
		return image;
	}
}
