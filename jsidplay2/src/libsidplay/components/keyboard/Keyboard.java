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
 * Implements the C64's keyboard.<br>
 * <br>
 * For documentation on the C64 keyboard handling, see <a
 * href='http://www.zimmers.net/anonftp/pub/cbm/c64/programming/documents/keymatrix.txt'>http://www.zimmers.net/anonftp/pub/cbm/c64/programming/documents/keymatrix.txt</a>
 * or <a
 * href='http://www.zimmers.net/anonftp/pub/cbm/magazines/transactor/v5i5/p039.jpg'>http://www.zimmers.net/anonftp/pub/cbm/magazines/transactor/v5i5/p039.jpg</a>.
 * 
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public abstract class Keyboard {
	private final Set<KeyTableEntry> keysDown = new HashSet<KeyTableEntry>();

	/** is CBM pressed? */
	private boolean cbm;

	/** Is left shift pressed ?*/
	private boolean leftShift;

	/** Is right shift pressed? */
	private boolean rightShift;

	/**
	 * Is any key forcing shift state?
	 * > 0 when we must pretend shift is down.
	 * < 0 when we must pretend shift is not down.
	 */
	private int forcedShift = 0;

	/**
	 * Reset the keyboard
	 */
	public synchronized void reset() {
		keysDown.clear();
		cbm = leftShift = rightShift = false;
		forcedShift = 0;
	}

	/**
	 * Handle a pressed key
	 * 
	 * @param key
	 *            key that was pressed
	 */
	public synchronized void keyPressed(final KeyTableEntry ktEntry) {
		if (keysDown.contains(ktEntry)) {
			return;
		}
		if (ktEntry.hasShiftPreference()) {
			forcedShift += ktEntry.shiftDown() ? 1 : -1;
		}
		if (ktEntry.hasCommodorePreference()) {
			cbm = true;
		}
		keysDown.add(ktEntry);
	}

	/**
	 * Handle a released key
	 * 
	 * @param key
	 *            key to release
	 */
	public synchronized void keyReleased(final KeyTableEntry ktEntry) {
		if (! keysDown.contains(ktEntry)) {
			return;
		}
		if (ktEntry.hasShiftPreference()) {
			forcedShift -= ktEntry.shiftDown() ? 1 : -1;
		}
		if (ktEntry.hasCommodorePreference()) {
			cbm = false;
		}
		keysDown.remove(ktEntry);
	}

	/**
	 * Get read adjustment for CIA 1 register PRA or PRB
	 * 
	 * @param testRegisterValue
	 *            register value from PRA or PRB
	 * @param matrix$
	 *            either activeColumnsMatrix or activeRowsMatrix
	 * @return read adjustment, to be AND connected to the normal register
	 *         output
	 */
	private synchronized byte readMatrix(final byte selected, final boolean wantRow) {
		/* temporarily adjust all matrices with controls for reading */
		if (cbm) {
			keyPressed(KeyTableEntry.COMMODORE);
		}

		if (forcedShift >= (rightShift ? 0 : 1)) {
			keyPressed(KeyTableEntry.SHIFT_RIGHT);
		}

		if (forcedShift >= (leftShift ? 0 : 1)) {
			keyPressed(KeyTableEntry.SHIFT_LEFT);
		}

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

		if (cbm) {
			keyReleased(KeyTableEntry.COMMODORE);
		}

		if (forcedShift >= (rightShift ? 0 : 1)) {
			keyReleased(KeyTableEntry.SHIFT_RIGHT);
		}

		if (forcedShift >= (leftShift ? 0 : 1)) {
			keyReleased(KeyTableEntry.SHIFT_LEFT);
		}

		return result;
	}

	/**
	 * Get read adjustment for CIA 1 register PRA
	 * 
	 * @param rows to read
	 * @return selected keyboard columns
	 */
	public byte readColumn(byte selected) {
		return readMatrix(selected, true);
	}

	/**
	 * Get read adjustment for CIA 1 register PRB
	 * @param selected 
	 * 
	 * @param selected columns to read
	 * @return selected keyboard rows
	 */
	public byte readRow(byte selected) {
		return readMatrix(selected, false);
	}

	public synchronized void cbm(final boolean b) {
		cbm = b;
	}

	public synchronized void leftShift(final boolean b) {
		leftShift = b;
	}

	public synchronized void rightShift(final boolean b) {
		rightShift = b;
	}
	
	/**
	 * Restore key pressed by user
	 */
	public abstract void restore();
}
