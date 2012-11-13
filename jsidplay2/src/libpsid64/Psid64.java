package libpsid64;

import static libpsid64.IPsidBoot.psid_boot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import libsidplay.Reloc65;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;

public class Psid64 {
	public static final String PACKAGE = "psid64";
	public static final String VERSION = "0.9";

	public static final int MAX_BLOCKS = 4;
	/**
	 * Number of memory pages.
	 */
	public static final int MAX_PAGES = 256;
	/**
	 * Driver without screen display.
	 */
	public static final int NUM_MINDRV_PAGES = 2;
	/**
	 * Driver with screen display.
	 */
	public static final int NUM_EXTDRV_PAGES = 5;
	/**
	 * Size of screen in pages.
	 */
	public static final int NUM_SCREEN_PAGES = 4;
	/**
	 * Size of char-set in pages.
	 */
	public static final int NUM_CHAR_PAGES = 8;
	/**
	 * Number of spaces before EOT.
	 */
	public static final int STIL_EOT_SPACES = 10;
	public static final String DOCUMENTS_STIL_TXT = "DOCUMENTS"
			+ System.getProperty("file.separator") + "STIL.txt";

	public static final String txt_relocOverlapsImage = "PSID64: relocation information overlaps the load image";
	public static final String txt_notEnoughC64Memory = "PSID64: C64 memory has no space for driver code";
	public static final String txt_fileIoError = "PSID64: File I/O error";
	public static final String txt_noSidTuneLoaded = "PSID64: No SID tune loaded";
	public static final String txt_noSidTuneConverted = "PSID64: No SID tune converted";

	//
	// configuration options
	//

	private boolean m_blankScreen;
	private int m_initialSong;
	private final boolean m_verbose;
	private String m_hvscRoot;

	private SidTune m_tune;
	private File m_file;
	private String m_statusString;
	private STILEntry m_stilEntry;

	//
	// conversion data
	//

	private Screen m_screen;
	private String m_stilText;
	/**
	 * Start page of driver, 0 means no driver.
	 */
	private short m_driverPage;
	/**
	 * Start page of screen, 0 means no screen.
	 */
	private short m_screenPage;
	/**
	 * Start page of chars, 0 means no chars.
	 */
	private short m_charPage;
	/**
	 * Start page of STIL, 0 means no STIL.
	 */
	private short m_stilPage;

	//
	// converted file
	//

	private byte m_programData[];
	private int m_programSize;
	private HashMap<String, Integer> globals;

	/**
	 * Structure to describe a memory block in the C64's memory map.
	 */
	protected static class block_t {
		protected int load;
		/** < start address */
		protected int size;
		/** < size of the memory block in bytes */
		protected byte[] data;
		protected int dataOff;
		/** < data to be stored */
		protected String description;
		/** < a short description */
	}

	public Psid64() {
		m_verbose = true;
		m_screen = new Screen();
	}

	public void setInitialSong(final int songNo) {
		m_initialSong = songNo;
	}

	public void setBlankScreen(final boolean blank) {
		m_blankScreen = blank;
	}

	public boolean load(final File file) {
		m_tune = null;
		try {
			m_tune = SidTune.load(file);
		} catch (final Exception e) {
			e.printStackTrace();
			m_statusString = txt_noSidTuneLoaded;
			m_file = null;
			return false;
		}
		if (m_tune == null) {
			m_statusString = txt_noSidTuneLoaded;
			m_file = null;
			return false;
		}

		m_tune.selectSong(0);
		m_file = file;

		return true;
	}

	public boolean load(final SidTune sidTune) {
		m_tune = sidTune;
		m_file = null;
		return true;
	}

	public boolean convert() {
		final block_t blocks[] = new block_t[MAX_BLOCKS];
		for (int i = 0; i < blocks.length; i++) {
			blocks[i] = new block_t();
		}
		int numBlocks;
		byte[] psid_mem;
		int driver_size;
		final int boot_size = psid_boot.length;
		int size;

		// handle special treatment of tunes programmed in BASIC
		final SidTuneInfo tuneInfo = m_tune.getInfo();
		if (tuneInfo.compatibility == SidTune.Compatibility.RSID_BASIC) {
			return convertBASIC();
		}

		// retrieve STIL entry for this SID tune
		if (!formatStilText()) {
			return false;
		}

		// find space for driver and screen (optional)
		findFreeSpace();
		if (m_driverPage == 0x00) {
			m_statusString = txt_notEnoughC64Memory;
			return false;
		}

		// use minimal driver if screen blanking is enabled
		if (m_blankScreen) {
			m_screenPage = (short) 0x00;
			m_charPage = (short) 0x00;
			m_stilPage = (short) 0x00;
		}

		// relocate and initialize the driver
		final DriverInfo driverInfo = new DriverInfo();
		initDriver(driverInfo);
		psid_mem = driverInfo.mem;
		final int reloc_driverPos = driverInfo.reloc_driverPos;
		driver_size = driverInfo.n;

		// fill the blocks structure
		numBlocks = 0;
		blocks[numBlocks].load = m_driverPage << 8;
		blocks[numBlocks].size = driver_size;
		blocks[numBlocks].data = psid_mem;
		blocks[numBlocks].dataOff = reloc_driverPos;
		blocks[numBlocks].description = "Driver code";
		++numBlocks;

		blocks[numBlocks].load = tuneInfo.loadAddr;
		blocks[numBlocks].size = tuneInfo.c64dataLen;
		final byte c64buf[] = new byte[65536];
		m_tune.placeProgramInMemory(c64buf);
		blocks[numBlocks].data = c64buf;
		blocks[numBlocks].dataOff = tuneInfo.loadAddr;
		System.arraycopy(c64buf, tuneInfo.loadAddr, blocks[numBlocks].data,
				blocks[numBlocks].dataOff, blocks[numBlocks].data.length
						- blocks[numBlocks].dataOff);
		blocks[numBlocks].description = "Music data";
		++numBlocks;

		if (m_screenPage != 0x00) {
			drawScreen();
			blocks[numBlocks].load = m_screenPage << 8;
			blocks[numBlocks].size = m_screen.getDataSize();
			blocks[numBlocks].data = m_screen.getData();
			blocks[numBlocks].dataOff = 0;
			blocks[numBlocks].description = "Screen";
			++numBlocks;
		}

		if (m_stilPage != 0x00) {
			blocks[numBlocks].load = m_stilPage << 8;
			blocks[numBlocks].size = m_stilText.length();
			blocks[numBlocks].data = new byte[m_stilText.length() + 1];
			for (int i = 0; i < m_stilText.length(); i++) {
				blocks[numBlocks].data[i] = (byte) m_stilText.charAt(i);
				blocks[numBlocks].dataOff = 0;
			}
			blocks[numBlocks].data[m_stilText.length()] = 0;
			blocks[numBlocks].description = "STIL text";
			++numBlocks;
		}

		Arrays.sort(blocks, 0, numBlocks, new Comparator<block_t>() {

			@Override
			public int compare(final block_t a, final block_t b) {
				if (a.load < b.load) {
					return -1;
				}
				if (a.load > b.load) {
					return 1;
				}

				return 0;

			}

		});

		// print memory map
		if (m_verbose) {
			int charset = m_charPage << 8;

			System.out.println("C64 memory map:");
			for (int i = 0; i < numBlocks; ++i) {
				if (charset != 0 && blocks[i].load > charset) {
					System.out.println("  $" + toHexWord(charset) + "-$"
							+ toHexWord(charset + 256 * NUM_CHAR_PAGES)
							+ "  Character set");
					charset = 0;
				}
				System.out.println("  $" + toHexWord(blocks[i].load) + "-$"
						+ toHexWord(blocks[i].load + blocks[i].size) + "  "
						+ blocks[i].description);
			}
			if (charset != 0) {
				System.out.println("  $" + toHexWord(charset) + "-$"
						+ toHexWord(charset + 256 * NUM_CHAR_PAGES)
						+ "  Character set");
			}
		}

		// calculate total size of the blocks
		size = 0;
		for (int i = 0; i < numBlocks; ++i) {
			size = size + blocks[i].size;
		}

		m_programSize = boot_size + size;
		m_programData = new byte[m_programSize];
		int destPos = 0;
		System.arraycopy(psid_boot, 0, m_programData, destPos, boot_size);

		// the value 0x0801 is related to start of the code in psidboot.a65
		int addr = 19;

		// fill in the initial song number (passed in at boot time)
		int song;
		song = m_programData[destPos + addr] & 0xff;
		song += (m_programData[destPos + addr + 1] & 0xff) << 8;
		song -= 0x0801 - 2;
		int initialSong;
		if (1 <= m_initialSong && m_initialSong <= tuneInfo.songs) {
			initialSong = m_initialSong;
		} else {
			initialSong = tuneInfo.startSong;
		}
		m_programData[destPos + song] = (byte) (initialSong - 1 & 0xff);

		final int eof = 0x0801 + boot_size - 2 + size;
		m_programData[destPos + addr++] = (byte) (eof & 0xff); // end of C64
																// file
		m_programData[destPos + addr++] = (byte) (eof >> 8);
		m_programData[destPos + addr++] = (byte) (0x10000 & 0xff); // end of
																	// high
																	// memory
		m_programData[destPos + addr++] = (byte) (0x10000 >> 8);
		m_programData[destPos + addr++] = (byte) (size + 0xff >> 8); // number
																		// of
																		// pages
																		// to
																		// copy
		m_programData[destPos + addr++] = (byte) (0x10000 - size & 0xff); // start
																			// of
																			// blocks
																			// after
																			// moving
		m_programData[destPos + addr++] = (byte) (0x10000 - size >> 8);
		m_programData[destPos + addr++] = (byte) (numBlocks - 1); // number of
																	// blocks -
																	// 1
		m_programData[destPos + addr++] = (byte) m_charPage; // page for
																// character
																// set, or 0
		final int jmpAddr = m_driverPage << 8;
		m_programData[destPos + addr++] = (byte) (jmpAddr & 0xff); // start
																	// address
																	// of driver
		m_programData[destPos + addr++] = (byte) (jmpAddr >> 8);
		m_programData[destPos + addr++] = (byte) (jmpAddr + 3 & 0xff); // address
																		// of
																		// new
																		// stop
																		// vector
		m_programData[destPos + addr++] = (byte) (jmpAddr + 3 >> 8); // for
																		// tunes
																		// that
																		// call
																		// $a7ae
																		// during
																		// init

		// copy block data to psidboot.a65 parameters
		for (int i = 0; i < numBlocks; ++i) {
			final int offs = addr + numBlocks - 1 - i;
			m_programData[destPos + offs] = (byte) (blocks[i].load & 0xff);
			m_programData[destPos + offs + MAX_BLOCKS] = (byte) (blocks[i].load >> 8);
			m_programData[destPos + offs + 2 * MAX_BLOCKS] = (byte) (blocks[i].size & 0xff);
			m_programData[destPos + offs + 3 * MAX_BLOCKS] = (byte) (blocks[i].size >> 8);
		}
		addr = addr + 4 * MAX_BLOCKS;
		destPos += boot_size;

		// copy blocks to c64 program file
		for (int i = 0; i < numBlocks; ++i) {
			System.arraycopy(blocks[i].data, blocks[i].dataOff, m_programData,
					destPos, blocks[i].size);
			destPos += blocks[i].size;
		}

		return true;
	}

	private boolean convertBASIC() {
		final SidTuneInfo tuneInfo = m_tune.getInfo();
		final int load = tuneInfo.loadAddr;
		final int end = load + tuneInfo.c64dataLen;
		final int bootCodeSize = 0;

		// allocate space for BASIC program and boot code (optional)
		m_programSize = 2 + tuneInfo.c64dataLen + bootCodeSize;
		m_programData = null;
		m_programData = new byte[m_programSize];

		// first the load address
		m_programData[0] = (byte) (load & 0xff);
		m_programData[1] = (byte) (load >> 8);

		// then copy the BASIC program
		final byte c64buf[] = new byte[65536];
		m_tune.placeProgramInMemory(c64buf);
		System.arraycopy(c64buf, load, m_programData, 2, tuneInfo.c64dataLen);

		// print memory map
		if (m_verbose) {
			System.out.println("C64 memory map:");
			System.out.println("  $" + toHexWord(load) + "-$" + toHexWord(end)
					+ "  BASIC program");
		}

		return true;
	}

	private String toHexWord(final int i) {
		return String.format("%04x", i);
	}

	protected static class DriverInfo {
		public byte[] mem;
		public int reloc_driverPos;
		public int n;
	}

	private DriverInfo initDriver(final DriverInfo result) {
		final byte[] driver;
		byte[] psid_mem;
		byte[] psid_reloc;
		int psid_size;
		int reloc_addr;
		int addr;
		int vsa; // video screen address
		int cba; // character memory base address

		result.reloc_driverPos = 0;
		result.n = 0;

		// select driver
		if (m_screenPage == 0x00) {
			psid_size = IPsidDrv.psid_driver.length;
			driver = IPsidDrv.psid_driver;
		} else {
			psid_size = IPsidExtDriver.psid_extdriver.length;
			driver = IPsidExtDriver.psid_extdriver;
		}

		// Relocation of C64 PSID driver code.
		psid_mem = psid_reloc = new byte[psid_size];
		System.arraycopy(driver, 0, psid_reloc, 0, psid_size);
		reloc_addr = m_driverPage << 8;

		// undefined references in the drive code need to be added to globals
		globals = new HashMap<String, Integer>();
		final int screen = m_screenPage << 8;
		globals.put("screen", screen);
		int screen_songnum = 0;
		final SidTuneInfo tuneInfo = m_tune.getInfo();
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
		globals.put("dd00", ((m_screenPage & 0xc0) >> 6 ^ 3 | 0x04));
		vsa = (short) ((m_screenPage & 0x3c) << 2);
		cba = (short) (m_charPage != 0 ? m_charPage >> 2 & 0x0e : 0x06);
		globals.put("d018", vsa | cba);

		ByteBuffer bp;
		final Reloc65 relocator = new Reloc65();
		if ((bp = relocator.reloc65(psid_reloc, psid_size, reloc_addr, globals)) == null) {
			System.err.println(PACKAGE + ": Relocation error.");
			return result;
		}
		psid_reloc = bp.array();
		final int reloc_driverPos = bp.position();
		psid_size = bp.limit();

		// Skip JMP table
		addr = 6;

		// Store parameters for PSID player.
		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.initAddr != 0 ? 0x4c
				: 0x60);
		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.initAddr & 0xff);
		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.initAddr >> 8);
		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.playAddr != 0 ? 0x4c
				: 0x60);
		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.playAddr & 0xff);
		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.playAddr >> 8);
		psid_reloc[reloc_driverPos + addr++] = (byte) tuneInfo.songs;

		// get the speed bits (the driver only has space for the first 32 songs)
		int speed = m_tune.getSongSpeedArray();
		psid_reloc[reloc_driverPos + addr++] = (byte) (speed & 0xff);
		psid_reloc[reloc_driverPos + addr++] = (byte) (speed >> 8 & 0xff);
		psid_reloc[reloc_driverPos + addr++] = (byte) (speed >> 16 & 0xff);
		psid_reloc[reloc_driverPos + addr++] = (byte) (speed >> 24);

		psid_reloc[reloc_driverPos + addr++] = (byte) (tuneInfo.loadAddr < 0x31a ? 0xff
				: 0x05);
		psid_reloc[reloc_driverPos + addr++] = iomap(tuneInfo.initAddr);
		psid_reloc[reloc_driverPos + addr++] = iomap(tuneInfo.playAddr);

		if (m_screenPage != 0x00) {
			psid_reloc[reloc_driverPos + addr++] = (byte) m_stilPage;
		}

		result.mem = psid_mem;
		result.reloc_driverPos = reloc_driverPos;
		result.n = psid_size;
		return result;
	}

	// Input: A 16-bit effective address
	// Output: A default bank-select value for $01.
	private byte iomap(final int addr) {
		// Force Real C64 Compatibility
		final SidTuneInfo tuneInfo = m_tune.getInfo();
		if (tuneInfo.compatibility == SidTune.Compatibility.RSID) {
			return 0; // Special case, converted to 0x37 later
		}

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
		return 0x34; // RAM only
	}

	private boolean addFlag(boolean hasFlags, final String flagName) {
		if (hasFlags) {
			m_screen.write(", ");
		} else {
			hasFlags = true;
		}
		m_screen.write(flagName);
		return hasFlags;
	}

	private void drawScreen() {
		m_screen.clear();

		// set title
		m_screen.move(5, 1);
		m_screen.write("PSID64 v" + VERSION + " by Roland Hermans!");

		// characters for color line effect
		m_screen.poke(4, 0, (short) 0x70);
		m_screen.poke(35, 0, (short) 0x6e);
		m_screen.poke(4, 1, (short) 0x5d);
		m_screen.poke(35, 1, (short) 0x5d);
		m_screen.poke(4, 2, (short) 0x6d);
		m_screen.poke(35, 2, (short) 0x7d);
		for (int i = 0; i < 30; ++i) {
			m_screen.poke(5 + i, 0, (short) 0x40);
			m_screen.poke(5 + i, 2, (short) 0x40);
		}

		// information lines
		m_screen.move(0, 4);
		m_screen.write("Name   : ");
		final SidTuneInfo tuneInfo = m_tune.getInfo();
		m_screen.write(tuneInfo.infoString[0].substring(0,
				Math.min(tuneInfo.infoString[0].length(), 31)));

		m_screen.write("\nAuthor : ");
		m_screen.write(tuneInfo.infoString[1].substring(0,
				Math.min(tuneInfo.infoString[1].length(), 31)));

		m_screen.write("\nRelease: ");
		m_screen.write(tuneInfo.infoString[2].substring(0,
				Math.min(tuneInfo.infoString[2].length(), 31)));

		m_screen.write("\nLoad   : $");
		m_screen.write(toHexWord(tuneInfo.loadAddr));
		m_screen.write("-$");
		m_screen.write(toHexWord(tuneInfo.loadAddr + tuneInfo.c64dataLen));

		m_screen.write("\nInit   : $");
		m_screen.write(toHexWord(tuneInfo.initAddr));

		m_screen.write("\nPlay   : ");
		if (tuneInfo.playAddr != 0) {
			m_screen.write("$");
			m_screen.write(toHexWord(tuneInfo.playAddr));
		} else {
			m_screen.write("N/A");
		}

		m_screen.write("\nSongs  : ");
		m_screen.write(String.format("%d", tuneInfo.songs));
		if (tuneInfo.songs > 1) {
			m_screen.write(" (now playing");
		}

		boolean hasFlags = false;
		m_screen.write("\nFlags  : ");
		if (tuneInfo.compatibility == SidTune.Compatibility.PSIDv1) {
			hasFlags = addFlag(hasFlags, "PlaySID");
		}
		hasFlags = addFlag(hasFlags, tuneInfo.clockSpeed.toString());
		hasFlags = addFlag(hasFlags, tuneInfo.sid1Model.toString());
		final int sid2midNibbles = (tuneInfo.sidChipBase2 >> 4) & 0xff;
		if (((sid2midNibbles & 1) == 0)
				&& (((0x42 <= sid2midNibbles) && (sid2midNibbles <= 0x7e)) || ((0xe0 <= sid2midNibbles) && (sid2midNibbles <= 0xfe)))) {
			hasFlags = addFlag(hasFlags, tuneInfo.sid2Model.toString()
					+ " at $" + toHexWord(tuneInfo.sidChipBase2));
		}
		if (!hasFlags) {
			m_screen.write("-");
		}
		m_screen.write("\nClock  :   :  :");

		// some additional text
		m_screen.write("\n\n  ");
		if (tuneInfo.songs <= 1) {
			m_screen.write("   [1");
		} else if (tuneInfo.songs <= 10) {
			m_screen.write("  [1-");
			m_screen.putchar(tuneInfo.songs % 10 + '0');
		} else if (tuneInfo.songs <= 11) {
			m_screen.write(" [1-0, A");
		} else {
			m_screen.write("[1-0, A-");
			m_screen.putchar(tuneInfo.songs <= 36 ? tuneInfo.songs - 11 + 'A'
					: 'Z');
		}
		m_screen.write("] Select song [+] Next song\n");
		m_screen.write("  [-] Previous song [DEL] Blank screen\n");
		if (tuneInfo.playAddr != 0) {
			m_screen.write("[~] Fast forward [LOCK] Show raster time\n");
		}
		m_screen.write("  [RUN/STOP] Stop [CTRL+CBM+DEL] Reset\n");

		// flashing bottom line (should be exactly 38 characters)
		m_screen.move(1, 24);
		m_screen.write("Website: http://psid64.sourceforge.net");
	}

	private boolean formatStilText() {
		m_stilText = "";

		String str = "";
		if (m_stilEntry == null && m_file != null) {
			final String name = PathUtils.getCollectionRelName(m_file,
					m_hvscRoot);
			if (null != name) {
				STIL stil = STIL.getInstance(m_hvscRoot);
				if (stil != null) {
					m_stilEntry = stil.getSTIL(name);
				}
			}
		}
		if (m_stilEntry != null) {
			str += writeEntry(m_stilEntry);
		}

		// convert the stil text and remove all double whitespace characters
		final int n = str.length();
		m_stilText = m_stilText.trim();

		// start the scroll text with some space characters (to separate end
		// from beginning and to make sure the color effect has reached the end
		// of the line before the first character is visible)
		for (int i = 0; i < STIL_EOT_SPACES - 1; ++i) {
			m_stilText += (char) Screen.iso2scr((short) ' ');
		}

		boolean space = true;
		boolean realText = false;
		for (int i = 0; i < n; ++i) {
			if (Character.isWhitespace(str.charAt(i))) {
				space = true;
			} else {
				if (space) {
					m_stilText += (char) Screen.iso2scr((short) ' ');
					space = false;
				}
				m_stilText += (char) Screen.iso2scr((short) str.charAt(i));
				realText = true;
			}
		}

		// check if the message contained at least one graphical character
		if (realText) {
			// end-of-text marker
			m_stilText += (char) 0xff;
		} else {
			// no STIL text at all
			m_stilText = "";
		}

		return true;
	}

	private String writeEntry(final STILEntry stilEntry) {
		final StringBuffer buffer = new StringBuffer();
		if (stilEntry.filename != null) {
			buffer.append("Filename: ");
			buffer.append(stilEntry.filename.trim());
			buffer.append(" - ");
		}
		if (stilEntry.globalComment != null) {
			buffer.append(stilEntry.globalComment.trim());
		}
		final Iterator<Info> infosIt = stilEntry.infos.iterator();
		for (final Iterator<Info> iterator = infosIt; iterator.hasNext();) {
			final Info info = iterator.next();
			if (info.comment != null) {
				buffer.append(info.comment.trim());
			}
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
		}
		int subTuneNo = 1;
		for (final TuneEntry entry : stilEntry.subtunes) {
			if (entry.globalComment != null) {
				buffer.append(entry.globalComment.trim());
			}
			final Iterator<Info> subtuneEntryIt = entry.infos.iterator();
			for (final Iterator<Info> subTuneIterator = subtuneEntryIt; subTuneIterator
					.hasNext();) {
				final Info info = subTuneIterator.next();
				buffer.append(" SubTune #" + subTuneNo + ": ");
				if (info.name != null) {
					buffer.append(" ");
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
			subTuneNo++;
		}
		return buffer.append("                                        ")
				.toString();
	}

	private short findStilSpace(final boolean pages[], final short scr,
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

	private void findFreeSpace()
	/*--------------------------------------------------------------------------*
	   In          : -
	   Out         : m_driverPage      startpage of driver, 0 means no driver
	       m_screenPage      startpage of screen, 0 means no screen
	       m_charPage      startpage of chars, 0 means no chars
	       m_stilPage      startpage of stil, 0 means no stil
	   Return value: -
	   Description : Find free space in the C64 memory map for the screen and the
	       driver code. Of course the driver code takes priority over
	       the screen.
	   Globals     : psid         PSID header and data
	   History     : 15-AUG-2001  RH  Initial version
	       21-SEP-2001  RH  Added support for screens located in the
	              memory ranges $4000-$8000 and $c000-$d000.
	 *--------------------------------------------------------------------------*/
	{
		final SidTuneInfo tuneInfo = m_tune.getInfo();
		final boolean pages[] = new boolean[MAX_PAGES];
		final int startp = tuneInfo.relocStartPage;
		final int maxp = tuneInfo.relocPages;
		int endp;
		int i;
		int j;
		int k;
		short bank;
		short scr;
		short chars;
		short driver;

		// calculate size of the STIL text in pages
		final short stilSize = (short) (m_stilText.length() + 255 >> 8);

		m_screenPage = (short) 0x00;
		m_driverPage = (short) 0x00;
		m_charPage = (short) 0x00;
		m_stilPage = (short) 0x00;

		if (startp == 0x00) {
			// Used memory ranges.
			final int used[] = { 0x00, 0x03, 0xa0, 0xbf, 0xd0, 0xff, 0x00, 0x00 // calculated
			// below
			};

			// Finish initialization by setting start and end pages.
			used[6] = tuneInfo.loadAddr >> 8;
			used[7] = tuneInfo.loadAddr + tuneInfo.c64dataLen - 1 >> 8;

			// Mark used pages in table.
			for (i = 0; i < MAX_PAGES; ++i) {
				pages[i] = false;
			}
			for (i = 0; i < used.length; i += 2) {
				for (j = used[i]; j <= used[i + 1]; ++j) {
					pages[j] = true;
				}
			}
		} else if (startp != 0xff && maxp != 0) {
			// the available pages have been specified in the PSID file
			endp = Math.min((startp + maxp), MAX_PAGES);

			// check that the relocation information does not use the following
			// memory areas: 0x0000-0x03FF, 0xA000-0xBFFF and 0xD000-0xFFFF
			if (startp < 0x04 || 0xa0 <= startp && startp <= 0xbf
					|| startp >= 0xd0 || endp - 1 < 0x04 || 0xa0 <= endp - 1
					&& endp - 1 <= 0xbf || endp - 1 >= 0xd0) {
				return;
			}

			for (i = 0; i < MAX_PAGES; ++i) {
				pages[i] = startp <= i && i < endp ? false : true;
			}
		} else {
			// not a single page is available
			return;
		}

		driver = 0;
		for (i = 0; i < 4; ++i) {
			// Calculate the VIC bank offset. Screens located inside banks 1 and
			// 3
			// require a copy the character rom in ram. The code below uses a
			// little trick to swap bank 1 and 2 so that bank 0 and 2 are
			// checked
			// before 1 and 3.
			bank = (short) (((i & 1 ^ i >> 1) != 0 ? i ^ 3 : i) << 6);

			for (j = 0; j < 0x40; j += 4) {
				// screen may not reside within the char rom mirror areas
				if ((bank & 0x40) == 0 && 0x10 <= j && j < 0x20) {
					continue;
				}

				// check if screen area is available
				scr = (short) (bank + j);
				if (pages[scr] || pages[scr + 1] || pages[scr + 2]
						|| pages[scr + 3]) {
					continue;
				}

				if ((bank & 0x40) != 0) {
					// The char rom needs to be copied to RAM so let's try to
					// find
					// a suitable location.
					for (k = 0; k < 0x40; k += 8) {
						// char rom area may not overlap with screen area
						if (k == (j & 0x38)) {
							continue;
						}

						// check if character rom area is available
						chars = (short) (bank + k);
						if (pages[chars] || pages[chars + 1]
								|| pages[chars + 2] || pages[chars + 3]
								|| pages[chars + 4] || pages[chars + 5]
								|| pages[chars + 6] || pages[chars + 7]) {
							continue;
						}

						driver = findDriverSpace(pages, scr, chars,
								NUM_EXTDRV_PAGES);
						if (driver != 0) {
							m_driverPage = driver;
							m_screenPage = scr;
							m_charPage = chars;
							if (stilSize != 0) {
								m_stilPage = findStilSpace(pages, scr, chars,
										driver, stilSize);
							}
							return;
						}
					}
				} else {
					driver = findDriverSpace(pages, scr, (short) 0,
							NUM_EXTDRV_PAGES);
					if (driver != 0) {
						m_driverPage = driver;
						m_screenPage = scr;
						if (stilSize != 0) {
							m_stilPage = findStilSpace(pages, scr, (short) 0,
									driver, stilSize);
						}
						return;
					}
				}
			}
		}

		if (driver == 0) {
			driver = findDriverSpace(pages, (short) 0, (short) 0,
					NUM_MINDRV_PAGES);
			m_driverPage = driver;
		}
	}

	public boolean save(final String fileName) {
		// Open binary output file stream.
		try {
			final OutputStream outfile = new FileOutputStream(fileName);
			return write(outfile);
		} catch (final IOException e) {
			m_statusString = txt_fileIoError + ": " + e.getMessage();
			return false;
		}
	}

	public boolean write(final OutputStream out) throws IOException {
		if (m_programData == null) {
			m_statusString = txt_noSidTuneConverted;
			return false;
		}

		for (int i = 0; i < m_programSize; i++) {
			out.write(m_programData[i]);
		}

		out.close();
		return true;
	}

	public void setHVSC(final String root) {
		m_hvscRoot = root;
	}

	public void setStilEntry(final STILEntry stilEntry) {
		m_stilEntry = stilEntry;
	}

	public String getStatus() {
		return m_statusString;
	}

}
