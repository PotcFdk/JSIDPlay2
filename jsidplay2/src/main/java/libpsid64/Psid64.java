package libpsid64;

import static libpsid64.IPsidBoot.psid_boot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import libsidplay.Reloc65;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;
import libsidutils.pucrunch.PUCrunch;

public class Psid64 {
	private static final String PACKAGE = "PSID64";
	private static final String VERSION = "0.9";

	/**
	 * Maximum memory block count required for tune driver
	 */
	private static final int MAX_BLOCKS = 4;
	/**
	 * Number of memory pages.
	 */
	private static final int MAX_PAGES = 256;
	/**
	 * Driver without screen display.
	 */
	private static final int NUM_MINDRV_PAGES = 2;
	/**
	 * Driver with screen display.
	 */
	private static final int NUM_EXTDRV_PAGES = 5;
	/**
	 * Size of screen in pages.
	 */
	private static final int NUM_SCREEN_PAGES = 4;
	/**
	 * Size of char-set in pages.
	 */
	private static final int NUM_CHAR_PAGES = 8;
	/**
	 * Number of spaces before EOT.
	 */
	private static final int STIL_EOT_SPACES = 10;

	private SidTune tune;
	private STILEntry stilEntry;
	private String tmpDir;
	private boolean verbose, blankScreen;

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setBlankScreen(boolean blankScreen) {
		this.blankScreen = blankScreen;
	}

	private byte[] convert() throws NotEnoughC64MemException {
		// handle special treatment of tunes programmed in BASIC
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.compatibility == SidTune.Compatibility.RSID_BASIC) {
			return convertBASIC();
		}

		// retrieve STIL entry for this SID tune
		String stilText = formatStilText().toString();

		// find space for driver and screen (optional)
		FreeMemPages freePages = findFreeSpace(stilText);

		// use minimal driver if screen blanking is enabled
		if (blankScreen) {
			freePages.setScreenPage((short) 0x00);
			freePages.setCharPage((short) 0x00);
			freePages.setStilPage((short) 0x00);
		}

		// relocate and initialize the driver
		final DriverInfo driverInfo = initDriver(freePages);
		// fill the blocks structure
		final MemoryBlock memBlocks[] = new MemoryBlock[MAX_BLOCKS];
		int numBlocks = 0;
		memBlocks[numBlocks] = new MemoryBlock();
		memBlocks[numBlocks].setStartAddress(freePages.getDriverPage() << 8);
		memBlocks[numBlocks].setSize(driverInfo.getSize());
		memBlocks[numBlocks].setData(driverInfo.getMemory());
		memBlocks[numBlocks].setDataOff(driverInfo.getRelocatedDriverPos());
		memBlocks[numBlocks].setDescription("Driver code");
		++numBlocks;

		final byte c64buf[] = new byte[65536];
		tune.placeProgramInMemory(c64buf);
		memBlocks[numBlocks] = new MemoryBlock();
		memBlocks[numBlocks].setStartAddress(tuneInfo.loadAddr);
		memBlocks[numBlocks].setSize(tuneInfo.c64dataLen);
		memBlocks[numBlocks].setData(c64buf);
		memBlocks[numBlocks].setDataOff(tuneInfo.loadAddr);
		memBlocks[numBlocks].setDescription("Music data");
		++numBlocks;

		if (freePages.getScreenPage() != 0x00) {
			Screen screen = drawScreen();
			memBlocks[numBlocks] = new MemoryBlock();
			memBlocks[numBlocks].setStartAddress(freePages.getScreenPage() << 8);
			memBlocks[numBlocks].setSize(screen.getDataSize());
			memBlocks[numBlocks].setData(screen.getData());
			memBlocks[numBlocks].setDataOff(0);
			memBlocks[numBlocks].setDescription("Screen");
			++numBlocks;
		}

		if (freePages.getStilPage() != 0x00) {
			byte[] data = new byte[stilText.length()];
			for (int i = 0; i < stilText.length(); i++) {
				data[i] = (byte) stilText.charAt(i);
			}
			memBlocks[numBlocks] = new MemoryBlock();
			memBlocks[numBlocks].setStartAddress(freePages.getStilPage() << 8);
			memBlocks[numBlocks].setSize(data.length);
			memBlocks[numBlocks].setData(data);
			memBlocks[numBlocks].setDataOff(0);
			memBlocks[numBlocks].setDescription("STIL text");
			++numBlocks;
		}
		Arrays.sort(memBlocks, 0, numBlocks, new MemoryBlockComparator());

		// print memory map
		if (verbose) {
			System.out.println("C64 memory map:");

			int charset = freePages.getCharPage() << 8;
			for (int i = 0; i < numBlocks; ++i) {
				if (charset != 0 && memBlocks[i].getStartAddress() > charset) {
					System.out.println("  $" + toHexWord(charset) + "-$"
							+ toHexWord(charset + 256 * NUM_CHAR_PAGES)
							+ "  Character set");
					charset = 0;
				}
				System.out.println("  $"
						+ toHexWord(memBlocks[i].getStartAddress())
						+ "-$"
						+ toHexWord(memBlocks[i].getStartAddress()
								+ memBlocks[i].getSize()) + "  "
						+ memBlocks[i].getDescription());
			}
			if (charset != 0) {
				System.out.println("  $" + toHexWord(charset) + "-$"
						+ toHexWord(charset + 256 * NUM_CHAR_PAGES)
						+ "  Character set");
			}
		}
		// calculate total size of the blocks
		int size = 0;
		for (int i = 0; i < numBlocks; ++i) {
			size += memBlocks[i].getSize();
		}
		byte[] programData = new byte[psid_boot.length + size];
		System.arraycopy(psid_boot, 0, programData, 0, psid_boot.length);

		// the value 0x0801 is related to start of the code in psidboot.a65
		int addr = 19;

		// fill in the initial song number (passed in at boot time)
		int song = programData[addr] & 0xff;
		song += (programData[addr + 1] & 0xff) << 8;
		song -= 0x0801 - 2;
		programData[song] = (byte) (tuneInfo.currentSong - 1 & 0xff);

		final int eof = 0x0801 + psid_boot.length - 2 + size;
		// end of C64 file
		programData[addr++] = (byte) (eof & 0xff);
		programData[addr++] = (byte) (eof >> 8);
		// end of high memory
		programData[addr++] = (byte) (0x10000 & 0xff);
		programData[addr++] = (byte) (0x10000 >> 8);
		// number of pages to copy
		programData[addr++] = (byte) (size + 0xff >> 8);
		// start of blocks after moving
		programData[addr++] = (byte) (0x10000 - size & 0xff);
		programData[addr++] = (byte) (0x10000 - size >> 8);
		// number of blocks - 1
		programData[addr++] = (byte) (numBlocks - 1);
		// page for character set, or 0
		programData[addr++] = (byte) freePages.getCharPage();
		final int jmpAddr = freePages.getDriverPage() << 8;
		// start address of driver
		programData[addr++] = (byte) (jmpAddr & 0xff);
		programData[addr++] = (byte) (jmpAddr >> 8);
		// address of new stop vector
		programData[addr++] = (byte) (jmpAddr + 3 & 0xff);
		// for tunes that call $a7ae during init
		programData[addr++] = (byte) (jmpAddr + 3 >> 8);

		// copy block data to psidboot.a65 parameters
		for (int i = 0; i < numBlocks; ++i) {
			final int offs = addr + numBlocks - 1 - i;
			programData[offs] = (byte) (memBlocks[i].getStartAddress() & 0xff);
			programData[offs + MAX_BLOCKS] = (byte) (memBlocks[i].getStartAddress() >> 8);
			programData[offs + 2 * MAX_BLOCKS] = (byte) (memBlocks[i].getSize() & 0xff);
			programData[offs + 3 * MAX_BLOCKS] = (byte) (memBlocks[i].getSize() >> 8);
		}
		addr = addr + 4 * MAX_BLOCKS;

		// copy blocks to c64 program file
		int destPos = psid_boot.length;
		for (int i = 0; i < numBlocks; ++i) {
			System.arraycopy(memBlocks[i].getData(), memBlocks[i].getDataOff(),
					programData, destPos, memBlocks[i].getSize());
			destPos += memBlocks[i].getSize();
		}
		return programData;
	}

	private byte[] convertBASIC() {
		final SidTuneInfo tuneInfo = tune.getInfo();
		// allocate space for BASIC program and boot code (optional)
		byte[] programData = new byte[2 + tuneInfo.c64dataLen + 0];

		// first the load address
		programData[0] = (byte) (tuneInfo.loadAddr & 0xff);
		programData[1] = (byte) (tuneInfo.loadAddr >> 8);

		// then copy the BASIC program
		final byte c64buf[] = new byte[65536];
		tune.placeProgramInMemory(c64buf);
		System.arraycopy(c64buf, tuneInfo.loadAddr, programData, 2,
				tuneInfo.c64dataLen);

		// print memory map
		if (verbose) {
			System.out.println("C64 memory map:");
			System.out.println("  $" + toHexWord(tuneInfo.loadAddr) + "-$"
					+ toHexWord(tuneInfo.loadAddr + tuneInfo.c64dataLen)
					+ "  BASIC program");
		}
		return programData;
	}

	private String toHexWord(final int i) {
		return String.format("%04x", i);
	}

	private DriverInfo initDriver(FreeMemPages freePages) {
		final DriverInfo result = new DriverInfo();

		// undefined references in the drive code need to be added to globals
		HashMap<String, Integer> globals = new HashMap<String, Integer>();
		final int screen = freePages.getScreenPage() << 8;
		globals.put("screen", screen);
		int screen_songnum = 0;
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.songs > 1) {
			screen_songnum = screen + 10 * 40 + 24;
			if (tuneInfo.songs >= 100) {
				++screen_songnum;
			}
			if (tuneInfo.songs >= 10) {
				++screen_songnum;
			}
		}
		globals.put("screen_songnum", screen_songnum);
		globals.put("dd00",
				((freePages.getScreenPage() & 0xc0) >> 6 ^ 3 | 0x04));
		// video screen address
		int vsa = (short) ((freePages.getScreenPage() & 0x3c) << 2);
		// character memory base address
		int cba = (short) (freePages.getCharPage() != 0 ? freePages
				.getCharPage() >> 2 & 0x0e : 0x06);
		globals.put("d018", vsa | cba);

		byte[] psidMem;
		// Relocation of C64 PSID driver code.
		// select driver
		if (freePages.getScreenPage() == 0x00) {
			psidMem = new byte[IPsidDrv.psid_driver.length];
			System.arraycopy(IPsidDrv.psid_driver, 0, psidMem, 0,
					psidMem.length);
		} else {
			psidMem = new byte[IPsidExtDriver.psid_extdriver.length];
			System.arraycopy(IPsidExtDriver.psid_extdriver, 0, psidMem, 0,
					psidMem.length);
		}
		ByteBuffer bp;
		final Reloc65 relocator = new Reloc65();
		if ((bp = relocator.reloc65(psidMem, psidMem.length,
				freePages.getDriverPage() << 8, globals)) == null) {
			throw new RuntimeException(PACKAGE + ": Relocation error.");
		}
		byte[] psidReloc = bp.array();
		final int relocDriverPos = bp.position();
		int psidRelocSize = bp.limit();

		// Skip JMP table
		int addr = 6;

		// Store parameters for PSID player.
		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.initAddr != 0 ? 0x4c
				: 0x60);
		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.initAddr & 0xff);
		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.initAddr >> 8);
		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.playAddr != 0 ? 0x4c
				: 0x60);
		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.playAddr & 0xff);
		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.playAddr >> 8);
		psidReloc[relocDriverPos + addr++] = (byte) tuneInfo.songs;

		// get the speed bits (the driver only has space for the first 32 songs)
		int speed = tune.getSongSpeedArray();
		psidReloc[relocDriverPos + addr++] = (byte) (speed & 0xff);
		psidReloc[relocDriverPos + addr++] = (byte) (speed >> 8 & 0xff);
		psidReloc[relocDriverPos + addr++] = (byte) (speed >> 16 & 0xff);
		psidReloc[relocDriverPos + addr++] = (byte) (speed >> 24);

		psidReloc[relocDriverPos + addr++] = (byte) (tuneInfo.loadAddr < 0x31a ? 0xff
				: 0x05);
		psidReloc[relocDriverPos + addr++] = iomap(tuneInfo.initAddr);
		psidReloc[relocDriverPos + addr++] = iomap(tuneInfo.playAddr);

		if (freePages.getScreenPage() != 0x00) {
			psidReloc[relocDriverPos + addr++] = (byte) freePages.getStilPage();
		}
		result.setMemory(psidMem);
		result.setRelocatedDriverPos(relocDriverPos);
		result.setSize(psidRelocSize);
		return result;
	}

	/**
	 * @param addr
	 *            A 16-bit effective address
	 * @return A default bank-select value for $01
	 */
	private byte iomap(final int addr) {
		// Force Real C64 Compatibility
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.compatibility == SidTune.Compatibility.RSID || addr == 0) {
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
		return 0x34; // RAM only
	}

	private Screen drawScreen() {
		Screen screen = new Screen();
		// set title
		screen.move(5, 1);
		screen.write(PACKAGE + " v" + VERSION + " by Roland Hermans!");

		// characters for color line effect
		screen.poke(4, 0, (short) 0x70);
		screen.poke(35, 0, (short) 0x6e);
		screen.poke(4, 1, (short) 0x5d);
		screen.poke(35, 1, (short) 0x5d);
		screen.poke(4, 2, (short) 0x6d);
		screen.poke(35, 2, (short) 0x7d);
		for (int i = 0; i < 30; ++i) {
			screen.poke(5 + i, 0, (short) 0x40);
			screen.poke(5 + i, 2, (short) 0x40);
		}

		// information lines
		screen.move(0, 4);
		screen.write("Name   : ");
		final SidTuneInfo tuneInfo = tune.getInfo();
		screen.write(tuneInfo.infoString[0].substring(0,
				Math.min(tuneInfo.infoString[0].length(), 31)));

		screen.write("\nAuthor : ");
		screen.write(tuneInfo.infoString[1].substring(0,
				Math.min(tuneInfo.infoString[1].length(), 31)));

		screen.write("\nRelease: ");
		screen.write(tuneInfo.infoString[2].substring(0,
				Math.min(tuneInfo.infoString[2].length(), 31)));

		screen.write("\nLoad   : $");
		screen.write(toHexWord(tuneInfo.loadAddr));
		screen.write("-$");
		screen.write(toHexWord(tuneInfo.loadAddr + tuneInfo.c64dataLen));

		screen.write("\nInit   : $");
		screen.write(toHexWord(tuneInfo.initAddr));

		screen.write("\nPlay   : ");
		if (tuneInfo.playAddr != 0) {
			screen.write("$");
			screen.write(toHexWord(tuneInfo.playAddr));
		} else {
			screen.write("N/A");
		}

		screen.write("\nSongs  : ");
		screen.write(String.format("%d", tuneInfo.songs));
		if (tuneInfo.songs > 1) {
			screen.write(" (now playing");
		}

		boolean hasFlags = false;
		screen.write("\nFlags  : ");
		if (tuneInfo.compatibility == SidTune.Compatibility.PSIDv1) {
			hasFlags = addFlag(screen, hasFlags, "PlaySID");
		}
		hasFlags = addFlag(screen, hasFlags, tuneInfo.clockSpeed.toString());
		hasFlags = addFlag(screen, hasFlags, tuneInfo.sid1Model.toString());
		final int sid2midNibbles = (tuneInfo.sidChipBase2 >> 4) & 0xff;
		if (((sid2midNibbles & 1) == 0)
				&& (((0x42 <= sid2midNibbles) && (sid2midNibbles <= 0x7e)) || ((0xe0 <= sid2midNibbles) && (sid2midNibbles <= 0xfe)))) {
			hasFlags = addFlag(screen, hasFlags, tuneInfo.sid2Model.toString()
					+ " at $" + toHexWord(tuneInfo.sidChipBase2));
		}
		if (!hasFlags) {
			screen.write("-");
		}
		screen.write("\nClock  :   :  :");

		// some additional text
		screen.write("\n\n  ");
		if (tuneInfo.songs <= 1) {
			screen.write("   [1");
		} else if (tuneInfo.songs <= 10) {
			screen.write("  [1-");
			screen.putchar(tuneInfo.songs % 10 + '0');
		} else if (tuneInfo.songs <= 11) {
			screen.write(" [1-0, A");
		} else {
			screen.write("[1-0, A-");
			screen.putchar(tuneInfo.songs <= 36 ? tuneInfo.songs - 11 + 'A'
					: 'Z');
		}
		screen.write("] Select song [+] Next song\n");
		screen.write("  [-] Previous song [DEL] Blank screen\n");
		if (tuneInfo.playAddr != 0) {
			screen.write("[~] Fast forward [LOCK] Show raster time\n");
		}
		screen.write("  [RUN/STOP] Stop [CTRL+CBM+DEL] Reset\n");

		// flashing bottom line (should be exactly 38 characters)
		screen.move(1, 24);
		screen.write("Website: http://psid64.sourceforge.net");
		return screen;
	}

	private boolean addFlag(Screen screen, boolean hasFlags,
			final String flagName) {
		if (hasFlags) {
			screen.write(", ");
		} else {
			hasFlags = true;
		}
		screen.write(flagName);
		return hasFlags;
	}

	private StringBuffer formatStilText() {
		StringBuffer result = new StringBuffer();
		StringBuffer stilText = new StringBuffer();

		if (stilEntry != null) {
			stilText.append(writeSTILEntry(stilEntry).trim());
		}

		// start the scroll text with some space characters (to separate end
		// from beginning and to make sure the color effect has reached the end
		// of the line before the first character is visible)
		for (int i = 0; i < STIL_EOT_SPACES - 1; ++i) {
			result.append(Screen.iso2scr(' '));
		}

		// replace multiple white spaces by exactly one space bar
		// and determine if the whole string is built on white-spaces
		boolean space = true;
		boolean realText = false;
		for (int i = 0; i < stilText.length(); ++i) {
			if (Character.isWhitespace(stilText.charAt(i))) {
				space = true;
			} else {
				if (space) {
					result.append(Screen.iso2scr(' '));
					space = false;
				}
				result.append(Screen.iso2scr(stilText.charAt(i)));
				realText = true;
			}
		}

		// check if the message contained at least one graphical character
		if (realText) {
			// end-of-text marker
			result.append((char) 0xff);
		} else {
			// no STIL text at all
			return new StringBuffer();
		}
		return result;
	}

	private String writeSTILEntry(final STILEntry stilEntry) {
		final StringBuffer buffer = new StringBuffer();
		if (stilEntry.filename != null) {
			buffer.append("Filename: ");
			buffer.append(stilEntry.filename.trim());
			buffer.append(" - ");
		}
		if (stilEntry.globalComment != null) {
			buffer.append(stilEntry.globalComment.trim());
		}
		for (Info info : stilEntry.infos) {
			getSTILInfo(buffer, info);
		}
		int subTuneNo = 1;
		for (final TuneEntry entry : stilEntry.subtunes) {
			if (entry.globalComment != null) {
				buffer.append(entry.globalComment.trim());
			}
			for (Info info : entry.infos) {
				buffer.append(" SubTune #" + subTuneNo + ": ");
				getSTILInfo(buffer, info);
			}
			subTuneNo++;
		}
		return buffer.append("                                        ")
				.toString();
	}

	private void getSTILInfo(final StringBuffer buffer, Info info) {
		if (info.name != null) {
			buffer.append(" Name: ");
			buffer.append(info.name.trim());
		}
		if (info.author != null) {
			buffer.append(" Author: ");
			buffer.append(info.author.trim());
		}
		if (info.title != null) {
			buffer.append(" Title: ");
			buffer.append(info.title.trim());
		}
		if (info.artist != null) {
			buffer.append(" Artist: ");
			buffer.append(info.artist.trim());
		}
		if (info.comment != null) {
			buffer.append(" Comment: ");
			buffer.append(info.comment.trim());
		}
	}

	/**
	 * Find free space in the C64 memory map for the screen and the driver code.
	 * Of course the driver code takes priority over the screen.
	 * 
	 * @return free mem pages for driver/screen/char/stil
	 * @throws NotEnoughC64MemException
	 *             no free memory for driver
	 */
	private FreeMemPages findFreeSpace(String stilText)
			throws NotEnoughC64MemException {
		// calculate size of the STIL text in pages
		final short stilSize = (short) (stilText.length() + 255 >> 8);

		final boolean pages[] = new boolean[MAX_PAGES];
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.relocStartPage == 0x00) {
			// Used memory ranges. calculated below
			final int used[] = { 0x00, 0x03, 0xa0, 0xbf, 0xd0, 0xff,
					tuneInfo.loadAddr >> 8,
					tuneInfo.loadAddr + tuneInfo.c64dataLen - 1 >> 8 };

			// Mark used pages in table.
			for (int i = 0; i < MAX_PAGES; ++i) {
				pages[i] = false;
			}
			for (int i = 0; i < used.length; i += 2) {
				for (int j = used[i]; j <= used[i + 1]; ++j) {
					pages[j] = true;
				}
			}
		} else if (tuneInfo.relocStartPage != 0xff && tuneInfo.relocPages != 0) {
			// the available pages have been specified in the PSID file
			int endp = Math.min(
					(tuneInfo.relocStartPage + tuneInfo.relocPages), MAX_PAGES);

			// check that the relocation information does not use the following
			// memory areas: 0x0000-0x03FF, 0xA000-0xBFFF and 0xD000-0xFFFF
			if (tuneInfo.relocStartPage < 0x04
					|| 0xa0 <= tuneInfo.relocStartPage
					&& tuneInfo.relocStartPage <= 0xbf
					|| tuneInfo.relocStartPage >= 0xd0 || endp - 1 < 0x04
					|| 0xa0 <= endp - 1 && endp - 1 <= 0xbf || endp - 1 >= 0xd0) {
				throw new NotEnoughC64MemException();
			}

			for (int i = 0; i < MAX_PAGES; ++i) {
				pages[i] = tuneInfo.relocStartPage <= i && i < endp ? false
						: true;
			}
		} else {
			// not a single page is available
			throw new NotEnoughC64MemException();
		}

		for (int i = 0; i < 4; ++i) {
			// Calculate the VIC bank offset. Screens located inside banks 1 and
			// 3 require a copy the character rom in ram. The code below uses a
			// little trick to swap bank 1 and 2 so that bank 0 and 2 are
			// checked before 1 and 3.
			short bank = (short) (((i & 1 ^ i >> 1) != 0 ? i ^ 3 : i) << 6);

			for (int j = 0; j < 0x40; j += 4) {
				// screen may not reside within the char rom mirror areas
				if ((bank & 0x40) == 0 && 0x10 <= j && j < 0x20) {
					continue;
				}

				// check if screen area is available
				short scr = (short) (bank + j);
				if (pages[scr] || pages[scr + 1] || pages[scr + 2]
						|| pages[scr + 3]) {
					continue;
				}

				if ((bank & 0x40) != 0) {
					// The char rom needs to be copied to RAM so let's try to
					// find a suitable location.
					for (int k = 0; k < 0x40; k += 8) {
						// char rom area may not overlap with screen area
						if (k == (j & 0x38)) {
							continue;
						}
						// check if character rom area is available
						short chars = (short) (bank + k);
						if (pages[chars] || pages[chars + 1]
								|| pages[chars + 2] || pages[chars + 3]
								|| pages[chars + 4] || pages[chars + 5]
								|| pages[chars + 6] || pages[chars + 7]) {
							continue;
						}
						short driver = findDriverSpace(pages, scr, chars,
								NUM_EXTDRV_PAGES);
						if (driver != 0) {
							FreeMemPages freePrages = new FreeMemPages();
							freePrages.setScreenPage(scr);
							freePrages.setCharPage(chars);
							if (stilSize != 0) {
								freePrages.setStilPage(findSTILSpace(pages,
										scr, chars, driver, stilSize));
							}
							return freePrages;
						}
					}
				} else {
					short driver = findDriverSpace(pages, scr, (short) 0,
							NUM_EXTDRV_PAGES);
					if (driver != 0) {
						FreeMemPages freePrages = new FreeMemPages();
						freePrages.setDriverPage(driver);
						freePrages.setScreenPage(scr);
						if (stilSize != 0) {
							freePrages.setStilPage(findSTILSpace(pages, scr,
									(short) 0, driver, stilSize));
						}
						return freePrages;
					}
				}
			}
		}
		short driver = findDriverSpace(pages, (short) 0, (short) 0,
				NUM_MINDRV_PAGES);
		if (driver != 0) {
			FreeMemPages freePrages = new FreeMemPages();
			freePrages.setDriverPage(driver);
			return freePrages;
		}
		throw new NotEnoughC64MemException();
	}

	private short findDriverSpace(final boolean pages[], final short scr,
			final short chars, final int size) {
		short firstPage = 0;
		for (int i = 0; i < MAX_PAGES; ++i) {
			if (pages[i] || scr != 0 && scr <= i && i < scr + NUM_SCREEN_PAGES
					|| chars != 0 && chars <= i && i < chars + NUM_CHAR_PAGES) {
				if (i - firstPage >= size) {
					return firstPage;
				}
				firstPage = (short) (i + 1);
			}
		}
		return 0;
	}

	private short findSTILSpace(final boolean pages[], final short scr,
			final short chars, final short driver, final int size) {
		int firstPage = 0;
		for (int i = 0; i < MAX_PAGES; ++i) {
			if (pages[i] || scr != 0 && scr <= i && i < scr + NUM_SCREEN_PAGES
					|| chars != 0 && chars <= i && i < chars + NUM_CHAR_PAGES
					|| driver <= i && i < driver + NUM_EXTDRV_PAGES) {
				if (i - firstPage >= size) {
					return (short) firstPage;
				}
				firstPage = i + 1;
			}
		}

		return 0;
	}

	public void convertFiles(STIL stil, final File[] files, final File target)
			throws NotEnoughC64MemException, IOException, SidTuneError {
		for (final File file : files) {
			if (file.isDirectory()) {
				convertFiles(stil, file.listFiles(), target);
			} else {
				convertToPSID64(stil, file, target);
			}
		}
	}

	private void convertToPSID64(STIL stil, final File file, final File target)
			throws NotEnoughC64MemException, IOException, SidTuneError {
		tune = SidTune.load(file);
		tune.selectSong(tune.getInfo().startSong);
		stilEntry = stil != null ? stil.getSTILEntry(file) : null;

		File tmpFile = new File(tmpDir, PathUtils.getBaseNameNoExt(file)
				+ ".prg.tmp");
		tmpFile.deleteOnExit();
		try (OutputStream outfile = new FileOutputStream(tmpFile)) {
			// convert to PSID64
			outfile.write(convert());
		}
		// crunch result
		new PUCrunch().run(new String[] {
				tmpFile.getAbsolutePath(),
				new File(target, PathUtils.getBaseNameNoExt(file) + ".prg")
						.getAbsolutePath() });
	}

}
