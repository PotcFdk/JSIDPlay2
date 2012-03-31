package libsidplay.components.keyboard;

/**
 * Data structure for an entry on the key table of the C64 keyboard
 */
public class KeyTableEntry {
	public static final KeyTableEntry ARROW_LEFT = new KeyTableEntry(7, 1);
	public static final KeyTableEntry ONE = new KeyTableEntry(7, 0);
	public static final KeyTableEntry TWO = new KeyTableEntry(7, 3);
	public static final KeyTableEntry THREE = new KeyTableEntry(1, 0);
	public static final KeyTableEntry FOUR = new KeyTableEntry(1, 3);
	public static final KeyTableEntry FIVE = new KeyTableEntry(2, 0);
	public static final KeyTableEntry SIX = new KeyTableEntry(2, 3);
	public static final KeyTableEntry SEVEN = new KeyTableEntry(3, 0);
	public static final KeyTableEntry EIGHT = new KeyTableEntry(3, 3);
	public static final KeyTableEntry NINE = new KeyTableEntry(4, 0);
	public static final KeyTableEntry ZERO = new KeyTableEntry(4, 3);
	public static final KeyTableEntry PLUS = new KeyTableEntry(5, 0);
	public static final KeyTableEntry MINUS = new KeyTableEntry(5, 3);
	public static final KeyTableEntry POUND = new KeyTableEntry(6, 0);
	public static final KeyTableEntry CLEAR_HOME = new KeyTableEntry(6, 3);
	public static final KeyTableEntry INS_DEL = new KeyTableEntry(0, 0);
	public static final KeyTableEntry CTRL = new KeyTableEntry(7, 2);
	public static final KeyTableEntry Q = new KeyTableEntry(7, 6);
	public static final KeyTableEntry W = new KeyTableEntry(1, 1);
	public static final KeyTableEntry E = new KeyTableEntry(1, 6);
	public static final KeyTableEntry R = new KeyTableEntry(2, 1);
	public static final KeyTableEntry T = new KeyTableEntry(2, 6);
	public static final KeyTableEntry Y = new KeyTableEntry(3, 1);
	public static final KeyTableEntry U = new KeyTableEntry(3, 6);
	public static final KeyTableEntry I = new KeyTableEntry(4, 1);
	public static final KeyTableEntry O = new KeyTableEntry(4, 6);
	public static final KeyTableEntry P = new KeyTableEntry(5, 1);
	public static final KeyTableEntry AT = new KeyTableEntry(5, 6);
	public static final KeyTableEntry STAR = new KeyTableEntry(6, 1);
	public static final KeyTableEntry ARROW_UP = new KeyTableEntry(6, 6);
	public static final KeyTableEntry RUN_STOP = new KeyTableEntry(7, 7);
	public static final KeyTableEntry A = new KeyTableEntry(1, 2);
	public static final KeyTableEntry S = new KeyTableEntry(1, 5);
	public static final KeyTableEntry D = new KeyTableEntry(2, 2);
	public static final KeyTableEntry F = new KeyTableEntry(2, 5);
	public static final KeyTableEntry G = new KeyTableEntry(3, 2);
	public static final KeyTableEntry H = new KeyTableEntry(3, 5);
	public static final KeyTableEntry J = new KeyTableEntry(4, 2);
	public static final KeyTableEntry K = new KeyTableEntry(4, 5);
	public static final KeyTableEntry L = new KeyTableEntry(5, 2);
	public static final KeyTableEntry COLON = new KeyTableEntry(5, 5);
	public static final KeyTableEntry SEMICOLON = new KeyTableEntry(6, 2);
	public static final KeyTableEntry EQUALS = new KeyTableEntry(6, 5);
	public static final KeyTableEntry RETURN = new KeyTableEntry(0, 1);
	public static final KeyTableEntry COMMODORE = new KeyTableEntry(7, 5);
	public static final KeyTableEntry SHIFT_LEFT = new KeyTableEntry(1, 7);
	public static final KeyTableEntry Z = new KeyTableEntry(1, 4);
	public static final KeyTableEntry X = new KeyTableEntry(2, 7);
	public static final KeyTableEntry C = new KeyTableEntry(2, 4);
	public static final KeyTableEntry V = new KeyTableEntry(3, 7);
	public static final KeyTableEntry B = new KeyTableEntry(3, 4);
	public static final KeyTableEntry N = new KeyTableEntry(4, 7);
	public static final KeyTableEntry M = new KeyTableEntry(4, 4);
	public static final KeyTableEntry COMMA = new KeyTableEntry(5, 7);
	public static final KeyTableEntry PERIOD = new KeyTableEntry(5, 4);
	public static final KeyTableEntry SLASH = new KeyTableEntry(6, 7);
	public static final KeyTableEntry SHIFT_RIGHT = new KeyTableEntry(6, 4);
	public static final KeyTableEntry CURSOR_UP_DOWN = new KeyTableEntry(0, 7);
	public static final KeyTableEntry CURSOR_LEFT_RIGHT = new KeyTableEntry(0, 2);
	public static final KeyTableEntry SPACE = new KeyTableEntry(7, 4);
	public static final KeyTableEntry F1 = new KeyTableEntry(0, 4);
	public static final KeyTableEntry F3 = new KeyTableEntry(0, 5);
	public static final KeyTableEntry F5 = new KeyTableEntry(0, 6);
	public static final KeyTableEntry F7 = new KeyTableEntry(0, 3);
	public static final KeyTableEntry RESTORE = new KeyTableEntry(-1, -1);

	/**
	 * key row, column and code
	 */
	private final int row,  col;

	/**
	 * Force commodore state for this key.
	 */
	private final Boolean autoshift;

	/**
	 * Force commodore state for this key.
	 */
	private final Boolean commodore;

	public KeyTableEntry(final int row, final int col) {
		this.row = row;
		this.col = col;
		this.autoshift = null;
		this.commodore = null;
	}
	/**
	 * Create a new KeyTableEntry
	 * @param   row row where the key is located
	 * @param   col column where the key is located
	 * @param   autoshift   automatically activate shift when the key is used?
	 */
	public KeyTableEntry(final KeyTableEntry in, final Boolean autoshift) {
		this.row = in.getRow();
		this.col = in.getCol();
		this.autoshift = autoshift;
		this.commodore = null;
	}
	public KeyTableEntry(final KeyTableEntry in, final Boolean autoshift, final Boolean commodore) {
		this.row = in.getRow();
		this.col = in.getCol();
		this.autoshift = autoshift;
		this.commodore = commodore;
	}

	@Override
	public final String toString() {
		return this.getClass().getName() + "( " + this.getRow() + ", " + this.getCol() + ", " + this.autoshift + ", " + this.commodore + " )";
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public boolean hasShiftPreference() {
		return autoshift != null;
	}

	public boolean hasCommodorePreference() {
		return commodore != null;
	}

	public boolean shiftDown() {
		return autoshift;
	}
	public boolean commodoreDown() {
		return commodore;
	}
}