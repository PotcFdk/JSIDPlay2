package libpsid64;

import static libpsid64.IPsidBoot.psid_boot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
		FreeMemPages freePages = findFreeSpace(stilText.length());

		// use minimal driver if screen blanking is enabled
		if (blankScreen) {
			freePages.setScreenPage(null);
			freePages.setCharPage(null);
			freePages.setStilPage(null);
		}

		// relocate and initialize the driver
		final DriverInfo driverInfo = initDriver(freePages);

		// fill the blocks structure
		final List<MemoryBlock> memBlocks = new ArrayList<>();
		MemoryBlock memoryBlock = new MemoryBlock();
		memoryBlock.setStartAddress(freePages.getDriverPage() << 8);
		memoryBlock.setSize(driverInfo.getSize());
		memoryBlock.setData(driverInfo.getMemory());
		memoryBlock.setDataOff(driverInfo.getRelocatedDriverPos());
		memoryBlock.setDescription("Driver code");
		memBlocks.add(memoryBlock);

		memoryBlock = new MemoryBlock();
		memoryBlock.setStartAddress(tuneInfo.loadAddr);
		memoryBlock.setSize(tuneInfo.c64dataLen);
		memoryBlock.setData(new byte[65536]);
		memoryBlock.setDataOff(tuneInfo.loadAddr);
		memoryBlock.setDescription("Music data");
		memBlocks.add(memoryBlock);
		tune.placeProgramInMemory(memoryBlock.getData());

		if (freePages.getScreenPage() != null) {
			Screen screen = drawScreen();
			memoryBlock = new MemoryBlock();
			memoryBlock.setStartAddress(freePages.getScreenPage() << 8);
			memoryBlock.setSize(screen.getDataSize());
			memoryBlock.setData(screen.getData());
			memoryBlock.setDataOff(0);
			memoryBlock.setDescription("Screen");
			memBlocks.add(memoryBlock);
		}

		if (freePages.getStilPage() != null) {
			byte[] data = new byte[stilText.length()];
			for (int i = 0; i < stilText.length(); i++) {
				data[i] = Screen.iso2scr(stilText.charAt(i));
			}
			data[data.length - 1] = (byte) 0xff;
			memoryBlock = new MemoryBlock();
			memoryBlock.setStartAddress(freePages.getStilPage() << 8);
			memoryBlock.setSize(data.length);
			memoryBlock.setData(data);
			memoryBlock.setDataOff(0);
			memoryBlock.setDescription("STIL text");
			memBlocks.add(memoryBlock);
		}
		Collections.sort(memBlocks, new MemoryBlockComparator());

		// print memory map
		if (verbose) {
			System.out.println("C64 memory map:");

			int charset = freePages.getCharPage() != null ? freePages
					.getCharPage() << 8 : 0;
			for (MemoryBlock memBlock : memBlocks) {
				if (charset != 0 && memBlock.getStartAddress() > charset) {
					System.out.println("  $" + toHexWord(charset) + "-$"
							+ toHexWord(charset + 256 * NUM_CHAR_PAGES)
							+ "  Character set");
					charset = 0;
				}
				System.out.println("  $"
						+ toHexWord(memBlock.getStartAddress())
						+ "-$"
						+ toHexWord(memBlock.getStartAddress()
								+ memBlock.getSize()) + "  "
						+ memBlock.getDescription());
			}
			if (charset != 0) {
				System.out.println("  $" + toHexWord(charset) + "-$"
						+ toHexWord(charset + 256 * NUM_CHAR_PAGES)
						+ "  Character set");
			}
		}
		// calculate total size of the blocks
		int size = 0;
		for (MemoryBlock memBlock : memBlocks) {
			size += memBlock.getSize();
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
		programData[addr++] = (byte) (memBlocks.size() - 1);
		// page for character set, or 0
		programData[addr++] = freePages.getCharPage() != null ? freePages
				.getCharPage().byteValue() : 0;
		final int jmpAddr = freePages.getDriverPage() != null ? freePages
				.getDriverPage() << 8 : 0;
		// start address of driver
		programData[addr++] = (byte) (jmpAddr & 0xff);
		programData[addr++] = (byte) (jmpAddr >> 8);
		// address of new stop vector
		programData[addr++] = (byte) (jmpAddr + 3 & 0xff);
		// for tunes that call $a7ae during init
		programData[addr++] = (byte) (jmpAddr + 3 >> 8);

		// copy block data to psidboot.a65 parameters
		int i = 0;
		for (MemoryBlock memBlock : memBlocks) {
			final int offs = addr + memBlocks.size() - 1 - (i++);
			programData[offs] = (byte) (memBlock.getStartAddress() & 0xff);
			programData[offs + MAX_BLOCKS] = (byte) (memBlock.getStartAddress() >> 8);
			programData[offs + 2 * MAX_BLOCKS] = (byte) (memBlock.getSize() & 0xff);
			programData[offs + 3 * MAX_BLOCKS] = (byte) (memBlock.getSize() >> 8);
		}
		addr = addr + 4 * MAX_BLOCKS;

		// copy blocks to c64 program file
		int destPos = psid_boot.length;
		for (MemoryBlock memBlock : memBlocks) {
			System.arraycopy(memBlock.getData(), memBlock.getDataOff(),
					programData, destPos, memBlock.getSize());
			destPos += memBlock.getSize();
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
		DriverInfo result = new DriverInfo();

		// undefined references in the drive code need to be added to globals
		HashMap<String, Integer> globals = new HashMap<String, Integer>();
		int screen = freePages.getScreenPage() != null ? freePages
				.getScreenPage() << 8 : 0;
		globals.put("screen", screen);
		int screen_songnum = 0;
		SidTuneInfo tuneInfo = tune.getInfo();
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
		int screenPage = freePages.getScreenPage() != null ? freePages
				.getScreenPage() : 0;
		globals.put("dd00", ((screenPage & 0xc0) >> 6 ^ 3 | 0x04));
		// video screen address
		int vsa = (screenPage & 0x3c) << 2;
		// character memory base address
		int cba = freePages.getCharPage() != null ? freePages.getCharPage() >> 2 & 0x0e
				: 0x06;
		globals.put("d018", vsa | cba);

		// select driver
		byte[] psidMem;
		if (freePages.getScreenPage() == null) {
			psidMem = new byte[IPsidDrv.psid_driver.length];
			System.arraycopy(IPsidDrv.psid_driver, 0, psidMem, 0,
					psidMem.length);
		} else {
			psidMem = new byte[IPsidExtDriver.psid_extdriver.length];
			System.arraycopy(IPsidExtDriver.psid_extdriver, 0, psidMem, 0,
					psidMem.length);
		}
		// Relocation of C64 PSID driver code.
		Reloc65 relocator = new Reloc65();
		ByteBuffer relocated = relocator.reloc65(psidMem, psidMem.length,
				freePages.getDriverPage() << 8, globals);
		if (relocated == null) {
			throw new RuntimeException(PACKAGE + ": Relocation error.");
		}
		storeDriverParameters(relocated.array(), relocated.position(),
				freePages, tuneInfo);
		result.setMemory(relocated.array());
		result.setRelocatedDriverPos(relocated.position());
		result.setSize(relocated.limit());
		return result;
	}

	private void storeDriverParameters(byte[] ram, int offset,
			FreeMemPages freePages, SidTuneInfo tuneInfo) {
		// Skip JMP table
		offset += 6;

		// Store parameters for PSID player.
		ram[offset++] = (byte) (tuneInfo.initAddr != 0 ? 0x4c : 0x60);
		ram[offset++] = (byte) (tuneInfo.initAddr & 0xff);
		ram[offset++] = (byte) (tuneInfo.initAddr >> 8);
		ram[offset++] = (byte) (tuneInfo.playAddr != 0 ? 0x4c : 0x60);
		ram[offset++] = (byte) (tuneInfo.playAddr & 0xff);
		ram[offset++] = (byte) (tuneInfo.playAddr >> 8);
		ram[offset++] = (byte) tuneInfo.songs;

		// get the speed bits (the driver only has space for the first 32 songs)
		int speed = tune.getSongSpeedArray();
		ram[offset++] = (byte) (speed & 0xff);
		ram[offset++] = (byte) (speed >> 8 & 0xff);
		ram[offset++] = (byte) (speed >> 16 & 0xff);
		ram[offset++] = (byte) (speed >> 24);

		ram[offset++] = (byte) (tuneInfo.loadAddr < 0x31a ? 0xff : 0x05);
		ram[offset++] = iomap(tuneInfo.initAddr);
		ram[offset++] = iomap(tuneInfo.playAddr);

		if (freePages.getScreenPage() != null) {
			ram[offset++] = freePages.getStilPage() != null ? freePages
					.getStilPage().byteValue() : 0;
		}
	}

	/**
	 * @param addr
	 *            A 16-bit effective address
	 * @return A default bank-select value for $01
	 */
	private byte iomap(int addr) {
		// Force Real C64 Compatibility
		SidTuneInfo tuneInfo = tune.getInfo();
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
		screen.poke(4, 0, 0x70);
		screen.poke(35, 0, 0x6e);
		screen.poke(4, 1, 0x5d);
		screen.poke(35, 1, 0x5d);
		screen.poke(4, 2, 0x6d);
		screen.poke(35, 2, 0x7d);
		for (int i = 0; i < 30; ++i) {
			screen.poke(5 + i, 0, 0x40);
			screen.poke(5 + i, 2, 0x40);
		}

		// information lines
		screen.move(0, 4);
		screen.write("Name   : ");
		SidTuneInfo tuneInfo = tune.getInfo();
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
		int sid2midNibbles = (tuneInfo.sidChipBase2 >> 4) & 0xff;
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

	private boolean addFlag(Screen screen, boolean hasFlags, String flagName) {
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
		if (stilEntry != null) {
			// append STIL infos,replace multiple whitespaces
			String writeSTILEntry = writeSTILEntry(stilEntry);
			String replaceAll = writeSTILEntry.replaceAll("([ \t\r\n])+", " ");
			result.append(replaceAll);
		}
		// check if the message contained at least one graphical character
		if (result.length() > 0) {
			// start the scroll text with some space characters (to separate end
			// from beginning and to make sure the color effect has reached the
			// end
			// of the line before the first character is visible)
			for (int i = 0; i < STIL_EOT_SPACES - 1; ++i) {
				result.insert(0, ' ');
			}
			// end-of-text marker
			result.append((char) 0xff);
		}
		return result;
	}

	private String writeSTILEntry(STILEntry stilEntry) {
		StringBuffer result = new StringBuffer();
		if (stilEntry.filename != null) {
			result.append("Filename: ");
			result.append(stilEntry.filename);
			result.append(" - ");
		}
		if (stilEntry.globalComment != null) {
			result.append(stilEntry.globalComment);
		}
		for (Info info : stilEntry.infos) {
			writeSTILEntry(result, info);
		}
		int subTuneNo = 1;
		for (TuneEntry entry : stilEntry.subtunes) {
			if (entry.globalComment != null) {
				result.append(entry.globalComment);
			}
			for (Info info : entry.infos) {
				result.append(" SubTune #" + subTuneNo + ": ");
				writeSTILEntry(result, info);
			}
			subTuneNo++;
		}
		return result.append("                                        ")
				.toString();
	}

	private void writeSTILEntry(StringBuffer buffer, Info info) {
		if (info.name != null) {
			buffer.append(" Name: ");
			buffer.append(info.name);
		}
		if (info.author != null) {
			buffer.append(" Author: ");
			buffer.append(info.author);
		}
		if (info.title != null) {
			buffer.append(" Title: ");
			buffer.append(info.title);
		}
		if (info.artist != null) {
			buffer.append(" Artist: ");
			buffer.append(info.artist);
		}
		if (info.comment != null) {
			buffer.append(" Comment: ");
			buffer.append(info.comment);
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
	private FreeMemPages findFreeSpace(int stilTextLength)
			throws NotEnoughC64MemException {
		// calculate size of the STIL text in pages
		int stilSize = stilTextLength + 255 >> 8;

		boolean pages[] = new boolean[MAX_PAGES];
		SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.relocStartPage == 0x00) {
			// Used memory ranges. calculated below
			int used[] = { 0x00, 0x03, 0xa0, 0xbf, 0xd0, 0xff,
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
			int bank = ((i & 1 ^ i >> 1) != 0 ? i ^ 3 : i) << 6;

			for (int j = 0; j < 0x40; j += 4) {
				// screen may not reside within the char rom mirror areas
				if ((bank & 0x40) == 0 && 0x10 <= j && j < 0x20) {
					continue;
				}

				// check if screen area is available
				int scr = bank + j;
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
						int chars = bank + k;
						if (pages[chars] || pages[chars + 1]
								|| pages[chars + 2] || pages[chars + 3]
								|| pages[chars + 4] || pages[chars + 5]
								|| pages[chars + 6] || pages[chars + 7]) {
							continue;
						}
						Integer driver = findSpace(pages, scr, chars, null,
								NUM_EXTDRV_PAGES);
						if (driver != null) {
							FreeMemPages freePrages = new FreeMemPages();
							freePrages.setDriverPage(driver);
							freePrages.setScreenPage(scr);
							freePrages.setCharPage(chars);
							if (stilSize != 0) {
								freePrages.setStilPage(findSpace(pages, scr,
										chars, driver, stilSize));
							}
							return freePrages;
						}
					}
				} else {
					Integer driver = findSpace(pages, scr, null, null,
							NUM_EXTDRV_PAGES);
					if (driver != null) {
						FreeMemPages freePages = new FreeMemPages();
						freePages.setDriverPage(driver);
						freePages.setScreenPage(scr);
						if (stilSize != 0) {
							freePages.setStilPage(findSpace(pages, scr, null,
									driver, stilSize));
						}
						return freePages;
					}
				}
			}
		}
		Integer driver = findSpace(pages, null, null, null, NUM_MINDRV_PAGES);
		if (driver != null) {
			FreeMemPages freePrages = new FreeMemPages();
			freePrages.setDriverPage(driver);
			return freePrages;
		}
		throw new NotEnoughC64MemException();
	}

	/**
	 * Try to find free consecutive memory pages.
	 * 
	 * @param pages
	 *            pages which are already marked as used
	 * @param scr
	 *            first screen page which is already used (not free)
	 * @param chars
	 *            first characters page which is already used (not free)
	 * @param driver
	 *            first driver page which is already used (not free)
	 * @param size
	 *            number of consecutive free memory pages to search for
	 * @return first page of free consecutive memory pages (null means not
	 *         found)
	 */
	private Integer findSpace(boolean pages[], Integer scr, Integer chars,
			Integer driver, int size) {
		int firstPage = 0;
		for (int i = 0; i < MAX_PAGES; ++i) {
			if (pages[i]
					|| (scr != null && scr <= i && i < scr + NUM_SCREEN_PAGES)
					|| (chars != null && chars <= i && i < chars
							+ NUM_CHAR_PAGES)
					|| (driver != null && driver <= i && i < driver
							+ NUM_EXTDRV_PAGES)) {
				if (i - firstPage >= size) {
					return firstPage;
				}
				firstPage = i + 1;
			}
		}
		return null;
	}

	public void convertFiles(STIL stil, File[] files, File target)
			throws NotEnoughC64MemException, IOException, SidTuneError {
		for (File file : files) {
			if (file.isDirectory()) {
				convertFiles(stil, file.listFiles(), target);
			} else {
				convertToPSID64(stil, file, target);
			}
		}
	}

	private void convertToPSID64(STIL stil, File file, File target)
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
