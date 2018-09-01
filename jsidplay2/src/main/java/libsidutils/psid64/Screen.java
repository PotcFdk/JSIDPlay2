package libsidutils.psid64;

import java.util.Iterator;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.Petscii;

public class Screen {
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
		hasFlags = addFlag(hasFlags, tuneInfo.getSIDModel(0).toString());
		int sid2midNibbles = (tuneInfo.getSIDChipBase(1) >> 4) & 0xff;
		if (((sid2midNibbles & 1) == 0) && (((0x42 <= sid2midNibbles) && (sid2midNibbles <= 0x7e))
				|| ((0xe0 <= sid2midNibbles) && (sid2midNibbles <= 0xfe)))) {
			hasFlags = addFlag(hasFlags,
					tuneInfo.getSIDModel(1).toString() + String.format(" at $%04x", tuneInfo.getSIDChipBase(1)));
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
			putchar(tuneInfo.getSongs() <= 36 ? tuneInfo.getSongs() - 11 + 'A' : 'Z');
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
			screen[i] = Petscii.iso88591ToPetscii(' ');
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
			screen[offs] = (byte) Petscii.iso88591ToPetscii((char) c);
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

	private int offset(int x, int y) {
		return x + (WIDTH * y);
	}

}
