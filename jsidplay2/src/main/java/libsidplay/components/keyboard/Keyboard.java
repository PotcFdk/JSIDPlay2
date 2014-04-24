/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation. For the full
 * license text, see http://www.gnu.org/licenses/gpl.html.
 */
package libsidplay.components.keyboard;

import java.util.HashSet;
import java.util.Set;

/**
 * <pre>
 * Implements the C64's keyboard.<br>
 * <br>
 * For documentation on the C64 keyboard handling, see
 * <a href='http://www.zimmers.net/anonftp/pub/cbm/c64/programming/documents/keymatrix.txt'>http://www.zimmers.net/anonftp/pub/cbm/c64/programming/documents/keymatrix.txt</a> or
 * <a href='http://www.zimmers.net/anonftp/pub/cbm/magazines/transactor/v5i5/p039.jpg'>http://www.zimmers.net/anonftp/pub/cbm/magazines/transactor/v5i5/p039.jpg</a>.
 * </pre>
 * 
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public abstract class Keyboard {
	private final Set<KeyTableEntry> keysDown = new HashSet<KeyTableEntry>();

	/**
	 * Reset the keyboard
	 */
	public synchronized void reset() {
		keysDown.clear();
	}

	/**
	 * Handle a pressed key
	 * 
	 * @param ktEntry
	 *            key that was pressed
	 */
	public synchronized void keyPressed(final KeyTableEntry ktEntry) {
		keysDown.add(ktEntry);
	}

	/**
	 * Handle a released key
	 * 
	 * @param ktEntry
	 *            key to release
	 */
	public synchronized void keyReleased(final KeyTableEntry ktEntry) {
		keysDown.remove(ktEntry);
	}

	/**
	 * Get read adjustment for CIA 1 register PRA or PRB
	 * 
	 * @param selected
	 * @param wantRow
	 *
	 * @return read adjustment, to be AND connected to the normal register
	 *         output
	 */
	private synchronized byte readMatrix(final byte selected,
			final boolean wantRow) {
		/* temporarily adjust all matrices with controls for reading */
		byte result = (byte) 0xff;
		for (KeyTableEntry kte : keysDown) {
			if (wantRow) {
				if ((selected & 1 << kte.getCol()) == 0) {
					result &= ~(1 << kte.getRow());
				}
			} else {
				if ((selected & 1 << kte.getRow()) == 0) {
					result &= ~(1 << kte.getCol());
				}
			}
		}
		return result;
	}

	/**
	 * Get read adjustment for CIA 1 register PRA
	 * 
	 * @param selected
	 *            Columns to read.
	 *
	 * @return selected keyboard columns
	 */
	public byte readColumn(byte selected) {
		return readMatrix(selected, true);
	}

	/**
	 * Get read adjustment for CIA 1 register PRB
	 *
	 * @param selected
	 *            columns to read
	 *
	 * @return selected keyboard rows
	 */
	public byte readRow(byte selected) {
		return readMatrix(selected, false);
	}

	/**
	 * Restore key pressed by user
	 */
	public abstract void restore();
}
