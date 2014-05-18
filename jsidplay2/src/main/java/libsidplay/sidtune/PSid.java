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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;

import javafx.application.Platform;
import javafx.scene.image.Image;
import libsidplay.Reloc65;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.mem.IPSIDDrv;

class PSid extends Prg {
	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties SID_AUTHORS = new Properties();
	static {
		try (InputStream is = SidTune.class
				.getResourceAsStream("pictures.properties")) {
			SID_AUTHORS.load(is);
		} catch (IOException e) {
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
		private static final int SIZE = 124;

		private PHeader(final byte[] header) {
			final ByteBuffer buffer = ByteBuffer.wrap(header);

			buffer.get(id);
			version = buffer.getShort();
			data = buffer.getShort();
			load = buffer.getShort();
			init = buffer.getShort();
			play = buffer.getShort();
			songs = buffer.getShort();
			start = buffer.getShort();
			speed = buffer.getInt();

			buffer.get(name);
			buffer.get(author);
			buffer.get(released);

			if (version >= 2) {
				flags = buffer.getShort();
				/* 2B */
				relocStartPage = buffer.get();
				relocPages = buffer.get();
				/* 2SID */
				sidChip2MiddleNybbles = buffer.get();
				reserved = buffer.get();
			}
		}

		private PHeader() {
		}

		private byte[] getArray() {
			final ByteBuffer buffer = ByteBuffer.allocate(SIZE);
			buffer.put(id);
			buffer.putShort(version);
			buffer.putShort(data);
			buffer.putShort(load);
			buffer.putShort(init);
			buffer.putShort(play);
			buffer.putShort(songs);
			buffer.putShort(start);
			buffer.putInt(speed);
			buffer.put(name);
			buffer.put(author);
			buffer.put(released);
			if (version >= 2) {
				buffer.putShort(flags);
				buffer.put(relocStartPage);
				buffer.put(relocPages);
				buffer.put(sidChip2MiddleNybbles);
				buffer.put(reserved);
			}
			return buffer.array();
		}

		/**
		 * Magic (PSID or RSID)
		 */
		private byte[] id = new byte[4];

		/**
		 * 0x0001, 0x0002 or 0x0003
		 */
		private short version;

		/**
		 * 16-bit offset to binary data in file
		 */
		private short data;

		/**
		 * 16-bit C64 address to load file to
		 */
		private short load;

		/**
		 * 16-bit C64 address of init subroutine
		 */
		private short init;

		/**
		 * 16-bit C64 address of play subroutine
		 */
		private short play;

		/**
		 * number of songs
		 */
		private short songs;

		/**
		 * start song out of [1..256]
		 */
		private short start;

		/**
		 * 32-bit speed info:<BR>
		 * 
		 * bit: 0=50 Hz, 1=CIA 1 Timer A (default: 60 Hz)
		 */
		private int speed;

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 * For version 0x0003, all 32 chars can be used without zero
		 * termination. if less than 32 chars are used then it should be
		 * terminated with a trailing zero
		 */
		private byte name[] = new byte[32];

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 * For version 0x0003, all 32 chars can be used without zero
		 * termination. if less than 32 chars are used then it should be
		 * terminated with a trailing zero
		 */
		private byte author[] = new byte[32];

		/**
		 * ASCII strings, 31 characters long and terminated by a trailing zero
		 * For version 0x0003, all 32 chars can be used without zero
		 * termination. if less than 32 chars are used then it should be
		 * terminated with a trailing zero
		 */
		private byte released[] = new byte[32];

		/**
		 * only version 0x0002+
		 */
		private short flags;

		/**
		 * only version 0x0002+
		 */
		private byte relocStartPage;

		/**
		 * only version 0x0002+
		 */
		private byte relocPages;

		/**
		 * only version 0x0002+ reserved for version 0x0002 used in version
		 * 0x0003 to indicate second SID chip address
		 */
		private byte sidChip2MiddleNybbles;

		/**
		 * only version 0x0002+
		 */
		private byte reserved;

		private String getString(byte[] info) {
			try (Scanner sc = new Scanner(new String(info, "ISO-8859-1"))) {
				if (sc.useDelimiter("\0").hasNext()) {
					return sc.next();
				}
			} catch (NoSuchElementException | UnsupportedEncodingException e) {
			}
			return "<?>";
		}

	}

	//
	// PSID_SPECIFIC and PSID_BASIC are mutually exclusive
	//

	private static final int PSID_MUS = 1 << 0;

	private static final int PSID_SPECIFIC = 1 << 1;

	private static final int PSID_BASIC = 1 << 1;

	private final Reloc65 relocator = new Reloc65();

	private ByteBuffer relocatedBuffer;

	private final byte[] driver = new byte[IPSIDDrv.PSIDDRV.length];

	private Image image;

	protected PSid() {
		System.arraycopy(IPSIDDrv.PSIDDRV, 0, driver, 0, driver.length);
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
	public int placeProgramInMemory(final byte[] mem) {
		super.placeProgramInMemory(mem);
		if (info.compatibility != Compatibility.RSID_BASIC) {
			return psidDrvReloc(mem);
		} else {
			mem[0x30c] = (byte) (info.currentSong - 1);
			return -1;
		}
	}

	private int psidDrvReloc(final byte[] mem) {
		final byte[] relocDriver = relocatedBuffer.array();
		final int relocDriverPos = relocatedBuffer.position();

		if (!(info.playAddr == 0 && info.loadAddr == 0x200)) {
			/*
			 * XXX: setting these vectors seems a bit dangerous because we will
			 * still run for some time
			 */
			mem[0x0314] = relocDriver[relocDriverPos + 2]; /* IRQ */
			mem[0x0315] = relocDriver[relocDriverPos + 2 + 1];
			if (info.compatibility != SidTune.Compatibility.RSID) {
				mem[0x0316] = relocDriver[relocDriverPos + 2 + 2]; /* Break */
				mem[0x0317] = relocDriver[relocDriverPos + 2 + 3];
				mem[0x0318] = relocDriver[relocDriverPos + 2 + 4]; /* NMI */
				mem[0x0319] = relocDriver[relocDriverPos + 2 + 5];
			}
		}
		int pos = info.determinedDriverAddr;

		/* Place driver into RAM */
		System.arraycopy(relocDriver, relocDriverPos + 10, mem, pos,
				info.determinedDriverLength);

		// Tell C64 about song
		mem[pos++] = (byte) (info.currentSong - 1);
		if (songSpeed[info.currentSong - 1] == Speed.VBI) {
			mem[pos] = 0;
		} else {
			// SIDTUNE_SPEED_CIA_1A
			mem[pos] = 1;
		}

		pos++;
		mem[pos++] = (byte) (info.initAddr & 0xff);
		mem[pos++] = (byte) (info.initAddr >> 8);
		mem[pos++] = (byte) (info.playAddr & 0xff);
		mem[pos++] = (byte) (info.playAddr >> 8);

		final int powerOnDelay = (int) (0x100 + (System.currentTimeMillis() & 0x1ff));
		mem[pos++] = (byte) (powerOnDelay & 0xff);
		mem[pos++] = (byte) (powerOnDelay >> 8);
		mem[pos++] = iomap(info.initAddr);
		mem[pos++] = iomap(info.playAddr);
		mem[pos + 1] = mem[pos + 0] = mem[0x02a6]; // PAL/NTSC flag
		pos++;

		// Add the required tune speed
		switch (info.clockSpeed) {
		case PAL:
			mem[pos++] = 1;
			break;
		case NTSC:
			mem[pos++] = 0;
			break;
		default: // UNKNOWN or ANY
			pos++;
			break;
		}

		// Default processor register flags on calling init
		if (info.compatibility == Compatibility.RSID) {
			mem[pos++] = 0;
		} else {
			mem[pos++] = 1 << MOS6510.SR_INTERRUPT;
		}

		return relocDriver[relocDriverPos + 0] & 0xff
				| (relocDriver[relocDriverPos + 1] & 0xff) << 8;
	}

	/**
	 * Common address resolution procedure
	 *
	 * @return True if the addresses could be resolved.
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
			info.loadAddr = (program[programOffset] & 0xff)
					+ ((program[programOffset + 1] & 0xff) << 8);
			programOffset += 2;
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

	protected static SidTune load(final String path, final byte[] dataBuf)
			throws SidTuneError {
		if (dataBuf.length < PHeader.SIZE) {
			throw new SidTuneError(String.format(
					"Header too short: %d, expected (%d)", dataBuf.length,
					PHeader.SIZE));
		}
		final PHeader header = new PHeader(dataBuf);

		final PSid psid = new PSid();
		psid.program = dataBuf;
		psid.programOffset = header.data;

		psid.info.c64dataLen = dataBuf.length - psid.programOffset;
		psid.info.loadAddr = header.load & 0xffff;
		psid.info.initAddr = header.init & 0xffff;
		psid.info.playAddr = header.play & 0xffff;

		psid.info.songs = header.songs & 0xffff;
		if (psid.info.songs == 0) {
			psid.info.songs++;
		}
		if (psid.info.songs > SIDTUNE_MAX_SONGS) {
			psid.info.songs = SIDTUNE_MAX_SONGS;
		}
		psid.info.startSong = header.start & 0xffff;
		if (psid.info.startSong > psid.info.songs) {
			psid.info.startSong = 1;
		} else if (psid.info.startSong == 0) {
			psid.info.startSong++;
		}

		int speed = header.speed;

		if (Arrays.equals(header.id, new byte[] { 'P', 'S', 'I', 'D' })) {
			switch (header.version) {
			case 1:
				psid.info.compatibility = Compatibility.PSIDv1;
				break;
			case 2:
				psid.info.compatibility = Compatibility.PSIDv2;
				if ((header.flags & PSID_SPECIFIC) != 0) {
					throw new SidTuneError(
							"PSID-specific files are not supported by this player");
				}
				break;
			case 3:
				psid.info.compatibility = Compatibility.PSIDv3;
				break;
			default:
				throw new SidTuneError("PSID version must be 1, 2 or 3, now: "
						+ header.version);
			}
		} else if (Arrays.equals(header.id, new byte[] { 'R', 'S', 'I', 'D' })) {
			if ((header.version < 2) || (header.version > 3)) {
				throw new SidTuneError("RSID version must be 2 or 3, now: "
						+ header.version);
			}
			psid.info.compatibility = (header.flags & PSID_BASIC) != 0 ? Compatibility.RSID_BASIC
					: Compatibility.RSID;

			if (psid.info.loadAddr != 0 || psid.info.playAddr != 0
					|| speed != 0) {
				throw new SidTuneError(
						"RSID tune specified load, play or speed information.");
			}
			speed = ~0; /* CIA */
		} else {
			throw new SidTuneError("Bad PSID header, expected (PSID or RSID)");
		}

		int clock = 0;
		int model1 = 0;
		int model2 = 0;
		if (header.version >= 2) {
			clock = (header.flags >> 2) & 3;
			model1 = (header.flags >> 4) & 3;

			psid.info.relocStartPage = (short) (header.relocStartPage & 0xff);
			psid.info.relocPages = (short) (header.relocPages & 0xff);

			if (header.version >= 3) {
				model2 = (header.flags >> 6) & 3;

				/* Handle 2nd SID chip location */
				int sid2loc = 0xd000 | (header.sidChip2MiddleNybbles & 0xff) << 4;
				if (((sid2loc >= 0xd420 && sid2loc < 0xd800) || sid2loc >= 0xde00)
						&& (sid2loc & 0x10) == 0) {
					psid.info.sidChipBase2 = sid2loc;
				}
			}
		}
		psid.info.clockSpeed = SidTune.Clock.values()[clock];
		psid.info.sid1Model = SidTune.Model.values()[model1];
		psid.info.sid2Model = SidTune.Model.values()[model2];

		// Create the speed/clock setting table.
		psid.convertOldStyleSpeedToTables(speed);

		// Name
		psid.info.infoString.add(header.getString(header.name));
		psid.info.infoString.add(header.getString(header.author));
		psid.info.infoString.add(header.getString(header.released));

		String photo = SID_AUTHORS.getProperty(header.getString(header.author));
		if (photo != null && Platform.isFxApplicationThread()) {
			photo = "Photos/" + photo;
			psid.image = new Image(SidTune.class.getResource(photo).toString());
		}

		if ((header.flags & PSID_MUS) != 0) {
			return new Mus(psid.info, psid.programOffset, dataBuf);
		}

		psid.resolveAddrs();
		psid.findPlaceForDriver();

		return psid;
	}

	@Override
	public void save(final String name, final boolean overWrite)
			throws IOException {
		try (FileOutputStream fos = new FileOutputStream(name, overWrite)) {
			final PHeader header = new PHeader();
			header.id = "PSID".getBytes();
			if (info.sidChipBase2 != 0) {
				header.version = 3;
			} else {
				header.version = 2;
			}
			header.data = PHeader.SIZE;
			header.songs = (short) info.songs;
			header.start = (short) info.startSong;
			header.speed = getSongSpeedArray();

			header.init = (short) info.initAddr;
			header.relocStartPage = (byte) info.relocStartPage;
			header.relocPages = (byte) info.relocPages;

			short tmpFlags = 0;
			switch (info.compatibility) {
			case RSID_BASIC:
				tmpFlags |= PSID_BASIC;
				//$FALL-THROUGH$

			case RSID:
				header.id = "RSID".getBytes();
				header.speed = 0;
				break;

			case PSIDv1:
				throw new IOException(
						"PSID-specific files are not supported by this player");

			default:
				header.play = (short) info.playAddr;
				break;
			}

			// @FIXME@ Need better solution. Make it possible to override MUS
			// strings
			if (info.infoString.size() == 3) {
				Iterator<String> descriptionIt = info.infoString.iterator();
				String title = descriptionIt.next();
				String author = descriptionIt.next();
				String released = descriptionIt.next();
				if (title.length() == 32 || author.length() == 32
						|| released.length() == 32) {
					header.version = 3;
				}
				for (int i = 0; i < title.length(); i++) {
					header.name[i] = (byte) title.charAt(i); // ISO-8859-1
				}
				for (int i = 0; i < author.length(); i++) {
					header.author[i] = (byte) author.charAt(i); // ISO-8859-1
				}
				for (int i = 0; i < released.length(); i++) {
					header.released[i] = (byte) released.charAt(i); // ISO-8859-1
				}
			}

			tmpFlags |= info.clockSpeed.ordinal() << 2;
			tmpFlags |= info.sid1Model.ordinal() << 4;
			tmpFlags |= info.sid2Model.ordinal() << 6;
			header.flags = tmpFlags;

			fos.write(header.getArray());

			final byte saveAddr[] = new byte[2];
			saveAddr[0] = (byte) (info.loadAddr & 255);
			saveAddr[1] = (byte) (info.loadAddr >> 8);
			fos.write(saveAddr);

			// Data starts at: bufferaddr + fileoffset
			// Data length: datafilelen - fileoffset
			fos.write(program, programOffset, info.c64dataLen);
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
		final byte[] myMD5 = new byte[info.c64dataLen + 6 + info.songs
				+ (info.clockSpeed == SidTune.Clock.NTSC ? 1 : 0)];
		System.arraycopy(program, programOffset, myMD5, 0, info.c64dataLen);
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
		final byte[] encryptMsg = MD5_DIGEST.digest(myMD5);
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
