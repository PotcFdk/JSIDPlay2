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

import static libsidplay.sidtune.PSidHeader.ISO_8859_1;
import static libsidplay.sidtune.PSidHeader.getString;
import static libsidplay.sidtune.SidTune.Clock.NTSC;
import static libsidplay.sidtune.SidTune.Clock.PAL;
import static libsidplay.sidtune.SidTune.Compatibility.PSIDv1;
import static libsidplay.sidtune.SidTune.Compatibility.PSIDv2;
import static libsidplay.sidtune.SidTune.Compatibility.PSIDv3;
import static libsidplay.sidtune.SidTune.Compatibility.PSIDv4;
import static libsidplay.sidtune.SidTune.Compatibility.RSID_BASIC;
import static libsidplay.sidtune.SidTune.Compatibility.RSIDv2;
import static libsidplay.sidtune.SidTune.Compatibility.RSIDv3;
import static libsidplay.sidtune.SidTune.Speed.CIA_1A;
import static libsidplay.sidtune.SidTune.Speed.VBI;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import libsidplay.components.mos6510.MOS6510;
import libsidutils.assembler.KickAssembler;
import libsidutils.assembler.KickAssemblerResult;
import libsidutils.reloc65.Reloc65;

class PSid extends Prg {

	private static final String PSID_DRIVER_ASM = "/libsidplay/sidtune/psiddriver.asm";

	private static final String PSID_DRIVER_BIN = "/libsidplay/sidtune/psiddriver.bin";

	//
	// PSID_SPECIFIC and PSID_BASIC are mutually exclusive
	//

	/**
	 * No more supported: MUS specific PSID files
	 */
	private static final int PSID_MUS = 1 << 0;

	/**
	 * No more supported: PSID specific PSID files
	 */
	private static final int PSID_SPECIFIC = 1 << 1;

	/**
	 * Compatibility: PSID BASIC files are handled like RSID_BASIC files
	 */
	private static final int PSID_BASIC = 1 << 1;

	/**
	 * PSID file format limit.
	 */
	private static final int SIDTUNE_MAX_SONGS = 256;

	private PSidHeader header;
	
	private Speed songSpeed[] = new Speed[SIDTUNE_MAX_SONGS];

	private KickAssemblerResult preparedDriver;

	@Override
	public Integer placeProgramInMemory(final byte[] mem) {
		super.placeProgramInMemory(mem);
		if (info.compatibility == RSID_BASIC) {
			mem[0x30c] = (byte) (info.currentSong - 1);
			return null;
		} else {
			if (USE_KICKASSEMBLER) {
				return assembleAndInstallDriver(mem);
			} else {
				return relocateAndInstallDriver(mem);
			}
		}
	}

	/**
	 * Linux ALSA is very sensible for timing: therefore we assemble before we open
	 * AudioLine
	 */
	public void prepare() {
		if (USE_KICKASSEMBLER) {
			HashMap<String, String> globals = new HashMap<String, String>();
			globals.put("pc", String.valueOf(info.determinedDriverAddr));
			globals.put("songNum", String.valueOf(info.currentSong));
			globals.put("songs", String.valueOf(info.songs));
			globals.put("songSpeed", String.valueOf(getSongSpeed(info.currentSong) == CIA_1A ? 1 : 0));
			globals.put("speed", String.valueOf(getSongSpeedWord()));
			globals.put("loadAddr", String.valueOf(info.loadAddr));
			globals.put("initAddr", String.valueOf(info.initAddr));
			globals.put("playAddr", String.valueOf(info.playAddr));
			globals.put("powerOnDelay", String.valueOf((int) (0x100 + (System.currentTimeMillis() & 0x1ff))));
			globals.put("initIOMap", String.valueOf(info.iomap(info.initAddr)));
			globals.put("playIOMap", String.valueOf(info.iomap(info.playAddr)));
			globals.put("videoMode", String.valueOf(info.clockSpeed == PAL ? 1 : 0));
			if (info.compatibility == RSIDv2 || info.compatibility == RSIDv3) {
				globals.put("flags", String.valueOf(1));
			} else {
				globals.put("flags", String.valueOf(1 << MOS6510.SR_INTERRUPT));
			}
			InputStream asm = PSid.class.getResourceAsStream(PSID_DRIVER_ASM);
			KickAssembler assembler = new KickAssembler();
			preparedDriver = assembler.assemble(PSID_DRIVER_ASM, asm, globals);
		}
	}

	private int assembleAndInstallDriver(final byte[] mem) {
		if (preparedDriver == null) {
			prepare();
		}
		info.determinedDriverLength = preparedDriver.getData().length - 2;
		System.arraycopy(preparedDriver.getData(), 2, mem, info.determinedDriverAddr, info.determinedDriverLength);
		if ((info.determinedDriverLength + 255) >> 8 != 1) {
			throw new RuntimeException("Driver must not be greater than one block! " + PSID_DRIVER_ASM);
		}
		Integer start = preparedDriver.getResolvedSymbols().get("start");
		if (start == null) {
			throw new RuntimeException("Label start not found in " + PSID_DRIVER_ASM);
		}
		return start;
	}

	private int relocateAndInstallDriver(final byte[] ram) {
		byte[] PSID_DRIVER;
		try (DataInputStream is = new DataInputStream(PSid.class.getResourceAsStream(PSID_DRIVER_BIN))) {
			URL url = PSid.class.getResource(PSID_DRIVER_BIN);
			PSID_DRIVER = new byte[url.openConnection().getContentLength()];
			is.readFully(PSID_DRIVER);
		} catch (IOException e) {
			throw new RuntimeException("Load failed for resource: " + PSID_DRIVER_BIN);
		}
		ByteBuffer relocatedBuffer = new Reloc65().reloc65(PSID_DRIVER, info.determinedDriverAddr - 10);

		if (relocatedBuffer == null) {
			throw new RuntimeException("Failed to relocate driver.");
		}
		info.determinedDriverLength = relocatedBuffer.limit() - 10;

		final byte[] reloc_driver = relocatedBuffer.array();
		final int reloc_driverPos = relocatedBuffer.position();

		if (!(info.playAddr == 0 && info.loadAddr == 0x200)) {
			/*
			 * Setting these vectors seems a bit dangerous because we will still run for
			 * some time
			 */
			ram[0x0314] = reloc_driver[reloc_driverPos + 2]; /* IRQ */
			ram[0x0315] = reloc_driver[reloc_driverPos + 2 + 1];
			if (!(info.compatibility == RSIDv2 || info.compatibility == RSIDv3)) {
				ram[0x0316] = reloc_driver[reloc_driverPos + 2 + 2]; /* Break */
				ram[0x0317] = reloc_driver[reloc_driverPos + 2 + 3];
				ram[0x0318] = reloc_driver[reloc_driverPos + 2 + 4]; /* NMI */
				ram[0x0319] = reloc_driver[reloc_driverPos + 2 + 5];
			}
		}
		int pos = info.determinedDriverAddr;

		/* Place driver into RAM */
		System.arraycopy(reloc_driver, reloc_driverPos + 10, ram, pos, info.determinedDriverLength);

		// Tell C64 about song
		ram[pos++] = (byte) (info.currentSong - 1);
		ram[pos++] = (byte) (songSpeed[info.currentSong - 1] == VBI ? 0 : 1);
		ram[pos++] = (byte) (info.initAddr & 0xff);
		ram[pos++] = (byte) (info.initAddr >> 8);
		ram[pos++] = (byte) (info.playAddr & 0xff);
		ram[pos++] = (byte) (info.playAddr >> 8);

		final int powerOnDelay = (int) (0x100 + (System.currentTimeMillis() & 0x1ff));
		ram[pos++] = (byte) (powerOnDelay & 0xff);
		ram[pos++] = (byte) (powerOnDelay >> 8);
		ram[pos++] = (byte) info.iomap(info.initAddr);
		ram[pos++] = (byte) info.iomap(info.playAddr);
		ram[pos++] = ram[0x02a6]; // Flag: TV Standard: $00 = NTSC, $01 = PAL.

		// Add the required tune speed
		switch (info.clockSpeed) {
		case PAL:
			ram[pos++] = 1;
			break;
		case NTSC:
			ram[pos++] = 0;
			break;
		default: // UNKNOWN or ANY? use clock speed of C64 system
			ram[pos++] = ram[0x02a6];
			break;
		}

		// Default processor register flags on calling init
		if (info.compatibility == RSIDv2 || info.compatibility == RSIDv3) {
			ram[pos++] = 0;
		} else {
			ram[pos++] = 1 << MOS6510.SR_INTERRUPT;
		}

		return reloc_driver[reloc_driverPos + 0] & 0xff | (reloc_driver[reloc_driverPos + 1] & 0xff) << 8;
	}

	/**
	 * Common address resolution procedure
	 *
	 * @throws SidTuneError
	 */
	private void resolveAddrs() throws SidTuneError {
		// Originally used as a first attempt at an RSID
		// style format. Now reserved for future use
		if (info.playAddr == 0xffff) {
			info.playAddr = 0;
		}
		// loadAddr = 0 means, the address is stored in front of the C64 data.
		if (info.loadAddr == 0) {
			if (info.c64dataLen < 2) {
				throw new SidTuneError("PSID: Song is truncated");
			}
			info.loadAddr = (program[programOffset] & 0xff) + ((program[programOffset + 1] & 0xff) << 8);
			programOffset += 2;
			info.c64dataLen -= 2;
		}
		if (info.compatibility == RSID_BASIC) {
			if (info.initAddr != 0) {
				throw new SidTuneError("PSID: Init address given for a RSID tune with BASIC flag");
			}
		} else if (info.initAddr == 0) {
			info.initAddr = info.loadAddr;
		}
	}

	/**
	 * Check for valid relocation information, and calculate place for driver. The
	 * driver is only 1 block long, and we currently make use of this knowledge.
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
				throw new SidTuneError(String.format(
						"PSID: Relocation info is invalid: end before start: end=%02x, start=%02x", endp, startp));
			}

			if (startp <= startlp && endp >= startlp || startp <= endlp && endp >= endlp) {
				throw new SidTuneError(String.format(
						"PSID: Relocation info is invalid: relocation in middle of song tune itself: songstart=%02x, songend=%02x, relocstart=%02x, relocend=%02x",
						startlp, endlp, startp, endp));
			}

			// Check that the relocation information does not use the following
			// memory areas: 0x0000-0x03FF, 0xA000-0xBFFF and 0xD000-0xFFFF
			if (startp < 0x04 || 0xa0 <= startp && startp <= 0xbf || startp >= 0xd0 || 0xa0 <= endp && endp <= 0xbf
					|| endp >= 0xd0) {
				throw new SidTuneError(String.format(
						"PSID: Relocation info is invalid: beyond acceptable bounds (kernal, basic, io, < 4th page): %02x-%02x",
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
			throw new SidTuneError("PSID: Can't relocate tune: no pages left to store driver.");
		}
	}

	protected static SidTune load(final String name, final byte[] dataBuf) throws SidTuneError {
		if (dataBuf.length < PSidHeader.SIZE) {
			throw new SidTuneError(
					String.format("PSID: Header too short: %d, expected (%d)", dataBuf.length, PSidHeader.SIZE));
		}
		final PSid psid = new PSid();
		psid.header = new PSidHeader(dataBuf);
		if ((psid.header.flags & PSID_MUS) != 0) {
			throw new SidTuneError("PSID: MUS-specific PSIDs are not supported by this player");
		}

		psid.program = dataBuf;
		psid.programOffset = psid.header.data;

		psid.info.c64dataLen = dataBuf.length - psid.programOffset;
		psid.info.loadAddr = psid.header.load & 0xffff;
		psid.info.initAddr = psid.header.init & 0xffff;
		psid.info.playAddr = psid.header.play & 0xffff;

		psid.info.songs = psid.header.songs & 0xffff;
		if (psid.info.songs == 0) {
			psid.info.songs++;
		}
		if (psid.info.songs > SIDTUNE_MAX_SONGS) {
			psid.info.songs = SIDTUNE_MAX_SONGS;
		}
		psid.info.startSong = psid.header.start & 0xffff;
		if (psid.info.startSong > psid.info.songs) {
			psid.info.startSong = 1;
		} else if (psid.info.startSong == 0) {
			psid.info.startSong++;
		}

		int speed = psid.header.speed;

		if (Arrays.equals(psid.header.id, "PSID".getBytes(ISO_8859_1))) {
			switch (psid.header.version) {
			case 1:
				psid.info.compatibility = PSIDv1;
				break;
			case 2:
				psid.info.compatibility = PSIDv2;
				if ((psid.header.flags & PSID_SPECIFIC) != 0) {
					throw new SidTuneError("PSID: PSID-specific files are not supported by this player");
				}
				break;
			case 3:
				psid.info.compatibility = PSIDv3;
				break;
			case 4:
				psid.info.compatibility = PSIDv4;
				break;
			default:
				throw new SidTuneError("PSID: PSID version must be 1, 2, 3 or 4, now: " + psid.header.version);
			}
		} else if (Arrays.equals(psid.header.id, "RSID".getBytes(ISO_8859_1))) {
			if ((psid.header.flags & PSID_BASIC) != 0) {
				psid.info.compatibility = RSID_BASIC;
			} else {
				switch (psid.header.version) {
				case 2:
					psid.info.compatibility = RSIDv2;
					break;
				case 3:
					psid.info.compatibility = RSIDv3;
					break;
				default:
					throw new SidTuneError("PSID: RSID version must be 2 or 3, now: " + psid.header.version);
				}
			}
			if (psid.info.loadAddr != 0 || psid.info.playAddr != 0 || speed != 0) {
				throw new SidTuneError("PSID: RSID tune specified load, play or speed information.");
			}
			speed = ~0; /* CIA */
		} else {
			throw new SidTuneError("PSID: Bad PSID header, expected (PSID or RSID)");
		}

		int clock = 0;
		int model1 = 0;
		int model2 = 0;
		int model3 = 0;
		if (psid.header.version >= 2) {
			clock = (psid.header.flags >> 2) & 3;
			model1 = (psid.header.flags >> 4) & 3;

			psid.info.relocStartPage = (short) (psid.header.relocStartPage & 0xff);
			psid.info.relocPages = (short) (psid.header.relocPages & 0xff);

		}
		if (psid.header.version >= 3) {
			model2 = (psid.header.flags >> 6) & 3;

			/* Handle 2nd SID chip location */
			int sid2loc = 0xd000 | (psid.header.sidChip2MiddleNybbles & 0xff) << 4;
			if (((sid2loc >= 0xd420 && sid2loc < 0xd800) || sid2loc >= 0xde00) && (sid2loc & 0x10) == 0) {
				psid.info.sidChipBase[1] = sid2loc;
				if (model2 == 0) {
					// If Unknown then SID will be same SID as the first SID
					model2 = model1;
				}
			}
		}
		if (psid.header.version >= 4) {
			model3 = (psid.header.flags >> 8) & 3;

			/* Handle 3rd SID chip location */
			int sid3loc = 0xd000 | (psid.header.sidChip3MiddleNybbles & 0xff) << 4;
			if (((sid3loc >= 0xd420 && sid3loc < 0xd800) || sid3loc >= 0xde00) && (sid3loc & 0x10) == 0) {
				psid.info.sidChipBase[2] = sid3loc;
				if (model3 == 0) {
					// If Unknown then SID will be same SID as the first SID
					model3 = model1;
				}
			}
		}
		psid.info.clockSpeed = Clock.values()[clock];
		psid.info.sidModel[0] = Model.values()[model1];
		psid.info.sidModel[1] = Model.values()[model2];
		psid.info.sidModel[2] = Model.values()[model3];

		// Create the speed/clock setting table.
		psid.convertOldStyleSpeedToTables(speed);

		// Name
		psid.info.infoString.add(getString(psid.header.name));
		psid.info.infoString.add(getString(psid.header.author));
		psid.info.infoString.add(getString(psid.header.released));

		psid.resolveAddrs();
		psid.findPlaceForDriver();

		return psid;
	}

	@Override
	public byte[] getTuneHeader() {
		return header.getArray();
	}
	
	/**
	 * Convert 32-bit PSID-style speed word to internal tables.
	 * 
	 * @param speed The speed to convert.
	 */
	private void convertOldStyleSpeedToTables(long speed) {
		for (int s = 0; s < SIDTUNE_MAX_SONGS; s++) {
			int i = s > 31 ? 31 : s;
			if ((speed & (1 << i)) != 0) {
				songSpeed[s] = CIA_1A;
			} else {
				songSpeed[s] = VBI;
			}
		}
	}

	@Override
	public int getSongSpeedWord() {
		int speed = 0;
		for (int i = 0; i < 32; ++i) {
			if (songSpeed[i] != VBI) {
				speed |= 1 << i;
			}
		}
		return speed;
	}

	@Override
	public Speed getSongSpeed(int selected) {
		return songSpeed[selected - 1];
	}

	@Override
	public void save(final String name) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(name)) {
			final PSidHeader header = new PSidHeader();
			header.id = "PSID".getBytes(ISO_8859_1);
			if (info.sidChipBase[2] != 0) {
				header.version = 4;
			} else if (info.sidChipBase[1] != 0) {
				header.version = 3;
			} else {
				header.version = 2;
			}

			header.data = PSidHeader.SIZE;
			header.songs = (short) info.songs;
			header.start = (short) info.startSong;
			header.speed = getSongSpeedWord();

			header.init = (short) info.initAddr;
			header.relocStartPage = (byte) info.relocStartPage;
			header.relocPages = (byte) info.relocPages;

			if (info.sidChipBase[1] != 0) {
				header.sidChip2MiddleNybbles = (byte) (info.sidChipBase[1] >> 4);
			}
			if (info.sidChipBase[2] != 0) {
				header.sidChip3MiddleNybbles = (byte) (info.sidChipBase[2] >> 4);
			}

			header.flags = 0;
			switch (info.compatibility) {
			case RSID_BASIC:
				header.flags |= PSID_BASIC;
				//$FALL-THROUGH$

			case RSIDv2:
			case RSIDv3:
				header.id = "RSID".getBytes(ISO_8859_1);
				header.speed = 0;
				break;

			case PSIDv1:
				throw new IOException("PSID-specific files are not supported by this player");

			default:
				header.play = (short) info.playAddr;
				break;
			}

			if (info.infoString.size() == 3) {
				Iterator<String> descriptionIt = info.infoString.iterator();
				String title = descriptionIt.next();
				String author = descriptionIt.next();
				String released = descriptionIt.next();
				if (header.version == 2 && title.length() == 32 || author.length() == 32 || released.length() == 32) {
					header.version = 3;
				}
				byte[] titleBytes = title.getBytes(ISO_8859_1);
				for (int i = 0; i < title.length(); i++) {
					header.name[i] = titleBytes[i];
				}
				byte[] authorBytes = author.getBytes(ISO_8859_1);
				for (int i = 0; i < author.length(); i++) {
					header.author[i] = authorBytes[i];
				}
				byte[] releasedBytes = released.getBytes(ISO_8859_1);
				for (int i = 0; i < released.length(); i++) {
					header.released[i] = releasedBytes[i];
				}
			}

			header.flags |= info.clockSpeed.ordinal() << 2;
			header.flags |= info.sidModel[0].ordinal() << 4;
			header.flags |= info.sidModel[1].ordinal() << 6;
			header.flags |= info.sidModel[2].ordinal() << 8;

			fos.write(header.getArray());

			final byte saveAddr[] = new byte[2];
			saveAddr[0] = (byte) (info.loadAddr & 255);
			saveAddr[1] = (byte) (info.loadAddr >> 8);
			fos.write(saveAddr);
			fos.write(program, programOffset, info.c64dataLen);
		}

	}

	/**
	 * Calculate MD5 checksum.
	 * 
	 * @return MD5 checksum as hex string
	 */
	@Override
	public String getMD5Digest(MD5Method md5Method) {
		if (md5Method == MD5Method.MD5_PSID_HEADER) {
			final byte[] myMD5 = new byte[info.c64dataLen + 6 + info.songs + (info.clockSpeed == NTSC ? 1 : 0)];
			System.arraycopy(program, programOffset, myMD5, 0, info.c64dataLen);
			int i = info.c64dataLen;
			myMD5[i++] = (byte) (info.initAddr & 0xff);
			myMD5[i++] = (byte) (info.initAddr >> 8);
			myMD5[i++] = (byte) (info.playAddr & 0xff);
			myMD5[i++] = (byte) (info.playAddr >> 8);
			myMD5[i++] = (byte) (info.songs & 0xff);
			myMD5[i++] = (byte) (info.songs >> 8);
			for (int s = 1; s <= info.songs; s++) {
				myMD5[i++] = (byte) getSongSpeed(s).speedValue();
			}
			// Deal with PSID v2NG clock speed flags: Let only NTSC
			// clock speed change the MD5 fingerprint. That way the
			// fingerprint of a PAL-speed sidtune in PSID v1, v2, and
			// PSID v2NG format is the same.
			if (info.clockSpeed == NTSC) {
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
		} else {
			// md5Method == MD5_CONTENTS
			return super.getMD5Digest(md5Method);
		}
	}

	@Override
	public long getInitDelay() {
		// 2.5ms does not always work well (e.g. RSIDs like Synth_sample)!
		return info.compatibility == RSID_BASIC || info.compatibility == RSIDv2 || info.compatibility == RSIDv3
				? RESET_INIT_DELAY
				: 2500;
	}

}
