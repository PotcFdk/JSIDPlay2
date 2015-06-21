package libpsid64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import sidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;
import libsidutils.assembler.KickAssembler;
import libsidutils.cruncher.PUCrunch;

//   psid64 - create a C64 executable from a PSID file
//   Copyright (C) 2001-2003  Roland Hermans <rolandh@users.sourceforge.net>
//
//   This program is free software// you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation// either version 2 of the License, or
//   (at your option) any later version.
//
//   This program is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY// without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with this program// if not, write to the Free Software
//   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//
//   The relocating PSID driver is based on a reference implementation written
//   by Dag Lem, using Andre Fachat's relocating cross assembler, xa. The
//   original driver code was introduced in VICE 1.7.
//
//   Please note that this driver code is optimized to squeeze the minimal
//   driver (without screen support) in just two memory pages. For this reason
//   it contains some strange branches to gain a few bytes. Look out for side
//   effects when updating this code!
public class Psid64 {
	private static final String PSID64_BOOT_ASM = "/libpsid64/psid64_boot.asm";
	private static final String PSID64_ASM = "/libpsid64/psid64.asm";
	private static final String PSID64_NOSCREEN_ASM = "/libpsid64/psid64_noscreen.asm";

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

	private KickAssembler assembler = new KickAssembler();
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

	private byte[] convert() {
		// handle special treatment of tunes programmed in BASIC
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.getCompatibility() == SidTune.Compatibility.RSID_BASIC) {
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
		final byte[] driverInfo = initDriver(freePages);

		// fill the blocks structure
		final List<MemoryBlock> memBlocks = new ArrayList<>();
		MemoryBlock memoryBlock = new MemoryBlock();
		memoryBlock.setStartAddress(freePages.getDriverPage() << 8);
		memoryBlock.setSize(driverInfo.length - 2);
		memoryBlock.setData(driverInfo);
		memoryBlock.setDataOff(2);
		memoryBlock.setDescription("Driver code");
		memBlocks.add(memoryBlock);

		memoryBlock = new MemoryBlock();
		memoryBlock.setStartAddress(tuneInfo.getLoadAddr());
		memoryBlock.setSize(tuneInfo.getC64dataLen());
		memoryBlock.setData(new byte[65536]);
		memoryBlock.setDataOff(tuneInfo.getLoadAddr());
		memoryBlock.setDescription("Music data");
		memBlocks.add(memoryBlock);
		tune.placeProgramInMemory(memoryBlock.getData());

		if (freePages.getScreenPage() != null) {
			Screen screen = new Screen(tune.getInfo());
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
		Collections.sort(memBlocks,
				(a, b) -> a.getStartAddress() - b.getStartAddress());
		final int driver = freePages.getDriverPage() != null ? freePages
				.getDriverPage() << 8 : 0;
		final int charset = freePages.getCharPage() != null ? freePages
				.getCharPage() << 8 : 0;

		printMemoryMap(memBlocks, charset);

		// calculate total size of the blocks
		int size = 0;
		for (MemoryBlock memBlock : memBlocks) {
			size += memBlock.getSize();
		}

		HashMap<String, String> globals = new HashMap<String, String>();
		globals.put("songNum", String.valueOf(tuneInfo.getCurrentSong() - 1));
		globals.put("size", String.valueOf(size));
		globals.put("numPages", String.valueOf(size + 0xff >> 8));
		globals.put("numBlocks", String.valueOf(memBlocks.size() - 1));
		globals.put("charPage", String.valueOf(charset));
		globals.put("driverPage", String.valueOf(driver));
		globals.put("stopVec", String.valueOf(driver + 3));
		for (int i = 0; i < MAX_BLOCKS; i++) {
			final boolean used = i < memBlocks.size();
			int blockNum = memBlocks.size() - i - 1 + (used ? 0 : MAX_BLOCKS);
			int blockStart = used ? memBlocks.get(i).getStartAddress() : 0;
			int blockSize = used ? memBlocks.get(i).getSize() : 0;
			globals.put("block" + blockNum + "Start",
					String.valueOf(blockStart));
			globals.put("block" + blockNum + "Size", String.valueOf(blockSize));
		}
		InputStream asm = Psid64.class.getResourceAsStream(PSID64_BOOT_ASM);
		byte[] psidBoot = assembler.assemble(PSID64_BOOT_ASM, asm, globals);
		byte[] programData = new byte[psidBoot.length + size];
		System.arraycopy(psidBoot, 0, programData, 0, psidBoot.length);

		// append memory blocks to PSID64 boot code
		int destPos = psidBoot.length;
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
		byte[] programData = new byte[2 + tuneInfo.getC64dataLen()];

		// first the load address
		programData[0] = (byte) (tuneInfo.getLoadAddr() & 0xff);
		programData[1] = (byte) (tuneInfo.getLoadAddr() >> 8);

		// then copy the BASIC program
		final byte c64buf[] = new byte[65536];
		tune.placeProgramInMemory(c64buf);
		System.arraycopy(c64buf, tuneInfo.getLoadAddr(), programData, 2,
				tuneInfo.getC64dataLen());

		printBasicMemoryMap(tuneInfo);

		return programData;
	}

	private void printBasicMemoryMap(final SidTuneInfo tuneInfo) {
		if (verbose) {
			final PrintStream out = System.out;
			out.println("C64 memory map:");
			out.printf("  $%04x-$%04x  BASIC program", tuneInfo.getLoadAddr(),
					tuneInfo.getLoadAddr() + tuneInfo.getC64dataLen());
			out.println();
		}
	}

	private void printMemoryMap(final List<MemoryBlock> memBlocks,
			final int charset) {
		if (verbose) {
			final PrintStream out = System.out;

			out.println("C64 memory map:");
			boolean charsetPrinted = false;
			for (MemoryBlock memBlock : memBlocks) {
				if (!charsetPrinted && memBlock.getStartAddress() > charset) {
					out.printf("  $%04x-$%04x  Character set", charset, charset
							+ 256 * NUM_CHAR_PAGES);
					out.println();
					charsetPrinted = true;
				}
				out.printf("  $%04x-$%04x  %s", memBlock.getStartAddress(),
						memBlock.getStartAddress() + memBlock.getSize(),
						memBlock.getDescription());
				out.println();
			}
			if (!charsetPrinted) {
				out.printf("  $%04x-$%04x  Character set", charset, charset
						+ 256 * NUM_CHAR_PAGES);
				out.println();
			}
		}
	}

	private byte[] initDriver(FreeMemPages freePages) {
		SidTuneInfo tuneInfo = tune.getInfo();

		int screenPage = freePages.getScreenPage() != null ? freePages
				.getScreenPage() : 0;
		int screen = screenPage << 8;
		int screenSongNum = 0;
		if (tuneInfo.getSongs() > 1) {
			screenSongNum = screen + 10 * 40 + 24;
			if (tuneInfo.getSongs() >= 100) {
				++screenSongNum;
			}
			if (tuneInfo.getSongs() >= 10) {
				++screenSongNum;
			}
		}
		// video screen address
		int vsa = (screenPage & 0x3c) << 2;
		// character memory base address
		int charset = freePages.getCharPage() != null ? freePages.getCharPage() >> 2 & 0x0e
				: 0x06;
		int stil = freePages.getStilPage() != null ? freePages.getStilPage()
				: 0;

		HashMap<String, String> globals = new HashMap<String, String>();
		globals.put("pc", String.valueOf(freePages.getDriverPage() << 8));
		globals.put("screen", String.valueOf(screen));
		globals.put("screen_songnum", String.valueOf(screenSongNum));
		globals.put("dd00", String.valueOf((screenPage & 0xc0) >> 6 ^ 3 | 0x04));
		globals.put("d018", String.valueOf(vsa | charset));
		globals.put("loadAddr", String.valueOf(tuneInfo.getLoadAddr()));
		globals.put("initAddr", String.valueOf(tuneInfo.getInitAddr()));
		globals.put("playAddr", String.valueOf(tuneInfo.getPlayAddr()));
		globals.put("songs", String.valueOf(tuneInfo.getSongs()));
		globals.put("speed", String.valueOf(tune.getSongSpeedArray()));
		globals.put("initIOMap", String.valueOf(iomap(tuneInfo.getInitAddr())));
		globals.put("playIOMap", String.valueOf(iomap(tuneInfo.getPlayAddr())));
		globals.put("stilPage", String.valueOf(stil));
		String resource;
		if (freePages.getScreenPage() == null) {
			resource = PSID64_NOSCREEN_ASM;
		} else {
			resource = PSID64_ASM;
		}
		InputStream asm = Psid64.class.getResourceAsStream(resource);
		return assembler.assemble(resource, asm, globals);
	}

	/**
	 * @param addr
	 *            A 16-bit effective address
	 * @return A default bank-select value for $01
	 */
	private int iomap(int addr) {
		// Force Real C64 Compatibility
		SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.getCompatibility() == SidTune.Compatibility.RSIDv2
				|| tuneInfo.getCompatibility() == SidTune.Compatibility.RSIDv3
				|| addr == 0) {
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
	 */
	private FreeMemPages findFreeSpace(int stilTextLength) {
		// calculate size of the STIL text in pages
		int stilSize = stilTextLength + 255 >> 8;

		boolean pages[] = new boolean[MAX_PAGES];
		SidTuneInfo tuneInfo = tune.getInfo();
		if (tuneInfo.getRelocStartPage() == 0x00) {
			// Used memory ranges. calculated below
			int used[] = { 0x00, 0x03, 0xa0, 0xbf, 0xd0, 0xff,
					tuneInfo.getLoadAddr() >> 8,
					tuneInfo.getLoadAddr() + tuneInfo.getC64dataLen() - 1 >> 8 };

			// Mark used pages in table.
			for (int i = 0; i < MAX_PAGES; ++i) {
				pages[i] = false;
			}
			for (int i = 0; i < used.length; i += 2) {
				for (int j = used[i]; j <= used[i + 1]; ++j) {
					pages[j] = true;
				}
			}
		} else if (tuneInfo.getRelocStartPage() != 0xff
				&& tuneInfo.getRelocPages() != 0) {
			// the available pages have been specified in the PSID file
			int endp = Math.min(
					(tuneInfo.getRelocStartPage() + tuneInfo.getRelocPages()),
					MAX_PAGES);

			// check that the relocation information does not use the following
			// memory areas: 0x0000-0x03FF, 0xA000-0xBFFF and 0xD000-0xFFFF
			if (tuneInfo.getRelocStartPage() < 0x04
					|| 0xa0 <= tuneInfo.getRelocStartPage()
					&& tuneInfo.getRelocStartPage() <= 0xbf
					|| tuneInfo.getRelocStartPage() >= 0xd0 || endp - 1 < 0x04
					|| 0xa0 <= endp - 1 && endp - 1 <= 0xbf || endp - 1 >= 0xd0) {
				throw new RuntimeException(
						"PSID64: Not enough memory for driver and screen!");
			}

			for (int i = 0; i < MAX_PAGES; ++i) {
				pages[i] = tuneInfo.getRelocStartPage() <= i && i < endp ? false
						: true;
			}
		} else {
			// not a single page is available
			throw new RuntimeException(
					"PSID64: Not enough memory for driver and screen!");
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
		throw new RuntimeException(
				"PSID64: Not enough memory for driver and screen!");
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

	public void convertFiles(Player player, File[] files, File target,
			File hvscRoot) throws IOException, SidTuneError {
		for (File file : files) {
			if (file.isDirectory()) {
				convertFiles(player, file.listFiles(), target, hvscRoot);
			} else {
				convertToPSID64(player, file, target, hvscRoot);
			}
		}
	}

	private void convertToPSID64(Player player, File file, File target,
			File hvscRoot) throws IOException, SidTuneError {
		tune = SidTune.load(file);
		tune.setSelectedSong(null);
		String collectionName = PathUtils.getCollectionName(hvscRoot,
				file.getPath());
		stilEntry = player.getStilEntry(collectionName);

		File tmpFile = new File(tmpDir, PathUtils.getBaseNameNoExt(file
				.getName()) + ".prg.tmp");
		tmpFile.deleteOnExit();
		try (OutputStream outfile = new FileOutputStream(tmpFile)) {
			// convert to PSID64
			outfile.write(convert());
		}
		// crunch result
		final PUCrunch puCrunch = new PUCrunch();
		puCrunch.setVerbose(verbose);
		puCrunch.crunch(tmpFile.getAbsolutePath(),
				new File(target, PathUtils.getBaseNameNoExt(file.getName())
						+ ".prg").getAbsolutePath());
	}

}
