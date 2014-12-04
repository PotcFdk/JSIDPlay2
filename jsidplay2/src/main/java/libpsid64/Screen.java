package libpsid64;

import java.util.Iterator;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;

public class Screen {
	private static final int[] scrtab = {
		0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, // 0x00
		0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f, // 0x08
		0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, // 0x10
		0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, // 0x18
		0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, // 0x20 !"#$%&'
		0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, // 0x28 ()*+,-./
		0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, // 0x30 01234567
		0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, // 0x38 89:;<=>?
		0x00, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, // 0x40 @ABCDEFG
		0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, // 0x48 HIJKLMNO
		0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, // 0x50 PQRSTUVW
		0x58, 0x59, 0x5a, 0x1b, 0xbf, 0x1d, 0x1e, 0x64, // 0x58 XYZ[\]^_
		0x27, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, // 0x60 `abcdefg
		0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, // 0x68 hijklmno
		0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, // 0x70 pqrstuvw
		0x18, 0x19, 0x1a, 0x1b, 0x5d, 0x1d, 0x1f, 0x20, // 0x78 xyz{|}~
		0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x80
		0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x88
		0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x90
		0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x98
		0x20, 0x21, 0x03, 0x1c, 0xbf, 0x59, 0x5d, 0xbf, // 0xa0  ¡¢£¤¥¦§
		0x22, 0x43, 0x01, 0x3c, 0xbf, 0x2d, 0x52, 0x63, // 0xa8 ¨©ª«¬­®¯
		0x0f, 0xbf, 0x32, 0x33, 0x27, 0x15, 0xbf, 0xbf, // 0xb0 °±²³´µ¶·
		0x2c, 0x31, 0x0f, 0x3e, 0xbf, 0xbf, 0xbf, 0x3f, // 0xb8 ¸¹º»¼½¾¿
		0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x43, // 0xc0 ÀÁÂÃÄÅÆÇ
		0x45, 0x45, 0x45, 0x45, 0x49, 0x49, 0x49, 0x49, // 0xc8 ÈÉÊËÌÍÎÏ
		0xbf, 0x4e, 0x4f, 0x4f, 0x4f, 0x4f, 0x4f, 0x18, // 0xd0 ÐÑÒÓÔÕÖ×
		0x4f, 0x55, 0x55, 0x55, 0x55, 0x59, 0xbf, 0xbf, // 0xd8 ØÙÚÛÜÝÞß
		0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x03, // 0xe0 àáâãäåæç
		0x05, 0x05, 0x05, 0x05, 0x09, 0x09, 0x09, 0x09, // 0xe8 èéêëìíîï
		0xbf, 0x0e, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0xbf, // 0xf0 ðñòóôõö÷
		0x0f, 0x15, 0x15, 0x15, 0x15, 0x19, 0xbf, 0x19 // 0xf8 øùúûüýþÿ	};
	};
	private static final String PACKAGE = "PSID64";
	private static final String VERSION = "0.9";

	private static final int WIDTH = 40;
	private static final int HEIGHT = 25;
	private static final int SCREEN_SIZE = WIDTH * HEIGHT;

	private byte screen[] = new byte[SCREEN_SIZE];
	private int x;
	private int y;

	public Screen(SidTuneInfo tuneInfo) {
		clear();
		// set title
		move(5, 1);
		write(PACKAGE + " v" + VERSION + " by Roland Hermans!");

		// characters for color line effect
		poke(4, 0, 0x70);
		poke(35, 0, 0x6e);
		poke(4, 1, 0x5d);
		poke(35, 1, 0x5d);
		poke(4, 2, 0x6d);
		poke(35, 2, 0x7d);
		for (int i = 0; i < 30; ++i) {
			poke(5 + i, 0, 0x40);
			poke(5 + i, 2, 0x40);
		}

		// information lines
		move(0, 4);
		write("Name   : ");
		Iterator<String> descriptionIt = tuneInfo.getInfoString().iterator();
		if (descriptionIt.hasNext()) {
			String title = descriptionIt.next();
			write(title.substring(0, Math.min(title.length(), 31)));
		}

		write("\nAuthor : ");
		if (descriptionIt.hasNext()) {
			String author = descriptionIt.next();
			write(author.substring(0, Math.min(author.length(), 31)));
		}

		write("\nRelease: ");
		if (descriptionIt.hasNext()) {
			String released = descriptionIt.next();
			write(released.substring(0, Math.min(released.length(), 31)));
		}
		write(String.format("\nLoad   : $%04x-$%04x", tuneInfo.getLoadAddr(),
				tuneInfo.getLoadAddr() + tuneInfo.getC64dataLen()));

		write(String.format("\nInit   : $%04x", tuneInfo.getInitAddr()));

		write("\nPlay   : ");
		if (tuneInfo.getPlayAddr() != 0) {
			write(String.format("$%04x", tuneInfo.getPlayAddr()));
		} else {
			write("N/A");
		}

		write("\nSongs  : ");
		write(String.format("%d", tuneInfo.getSongs()));
		if (tuneInfo.getSongs() > 1) {
			write(" (now playing");
		}

		boolean hasFlags = false;
		write("\nFlags  : ");
		if (tuneInfo.getCompatibility() == SidTune.Compatibility.PSIDv1) {
			hasFlags = addFlag(hasFlags, "PlaySID");
		}
		hasFlags = addFlag(hasFlags, tuneInfo.getClockSpeed().toString());
		hasFlags = addFlag(hasFlags, tuneInfo.getSid1Model().toString());
		int sid2midNibbles = (tuneInfo.getSidChipBase2() >> 4) & 0xff;
		if (((sid2midNibbles & 1) == 0)
				&& (((0x42 <= sid2midNibbles) && (sid2midNibbles <= 0x7e)) || ((0xe0 <= sid2midNibbles) && (sid2midNibbles <= 0xfe)))) {
			hasFlags = addFlag(hasFlags, tuneInfo.getSid2Model().toString()
					+ String.format(" at $%04x", tuneInfo.getSidChipBase2()));
		}
		if (!hasFlags) {
			write("-");
		}
		write("\nClock  :   :  :");

		// some additional text
		write("\n\n  ");
		if (tuneInfo.getSongs() <= 1) {
			write("   [1");
		} else if (tuneInfo.getSongs() <= 10) {
			write("  [1-");
			putchar(tuneInfo.getSongs() % 10 + '0');
		} else if (tuneInfo.getSongs() <= 11) {
			write(" [1-0, A");
		} else {
			write("[1-0, A-");
			putchar(tuneInfo.getSongs() <= 36 ? tuneInfo.getSongs() - 11 + 'A'
					: 'Z');
		}
		write("] Select song [+] Next song\n");
		write("  [-] Previous song [DEL] Blank screen\n");
		if (tuneInfo.getPlayAddr() != 0) {
			write("[~] Fast forward [LOCK] Show raster time\n");
		}
		write("  [RUN/STOP] Stop [CTRL+CBM+DEL] Reset\n");

		// flashing bottom line (should be exactly 38 characters)
		move(1, 24);
		write("Website: http://psid64.sourceforge.net");
	}

	private boolean addFlag(boolean hasFlags, String flagName) {
		if (hasFlags) {
			write(", ");
		} else {
			hasFlags = true;
		}
		write(flagName);
		return hasFlags;
	}

	private void clear() {
		for (int i = 0; i < SCREEN_SIZE; ++i) {
			screen[i] = iso2scr(' ');
		}
	}

	private void move(int x, int y) {
		if ((x < WIDTH) && (y < HEIGHT)) {
			this.x = x;
			this.y = y;
		}
	}

	private void putchar(int c) {
		if (c == '\n') {
			x = 0;
			moveDown();
		} else {
			int offs = offset(x, y);
			screen[offs] = (byte) iso2scr((char) c);
			moveRight();
		}
	}

	private void write(String str) {
		for (int i = 0; i < str.length(); i++) {
			putchar(str.charAt(i));
		}
	}

	private void poke(int x, int y, int value) {
		if ((x < WIDTH) && (y < HEIGHT)) {
			int offs = offset(x, y);
			screen[offs] = (byte) (value & 0xff);
		}
	}

	public final byte[] getData() {
		return screen;
	}

	public int getDataSize() {
		return SCREEN_SIZE;
	}

	private void moveRight() {
		if (x < (WIDTH - 1)) {
			++x;
		}
	}

	private void moveDown() {
		if (y < (HEIGHT - 1)) {
			++y;
		}
	}

	public static byte iso2scr(char c) {
		return (byte) scrtab[c & 0xff];
	}

	private int offset(int x, int y) {
		return x + (WIDTH * y);
	}

}
