package applet.videoscreen;

import libsidplay.components.keyboard.KeyTableEntry;

public class CommodoreKeyBoard extends NormalKeyBoard {

	protected Key[] firstRow = new Key[] {
			new Key(String.valueOf((char) 0x5f), new KeyTableEntry(KeyTableEntry.ARROW_LEFT, false, true), 32),
			new Key(String.valueOf((char) 0x31), new KeyTableEntry(KeyTableEntry.ONE, false, true), 32),
			new Key(String.valueOf((char) 0x32), new KeyTableEntry(KeyTableEntry.TWO, false, true), 32),
			new Key(String.valueOf((char) 0x33), new KeyTableEntry(KeyTableEntry.THREE, false, true), 32),
			new Key(String.valueOf((char) 0x34), new KeyTableEntry(KeyTableEntry.FOUR, false, true), 32),
			new Key(String.valueOf((char) 0x35), new KeyTableEntry(KeyTableEntry.FIVE, false, true), 32),
			new Key(String.valueOf((char) 0x36), new KeyTableEntry(KeyTableEntry.SIX, false, true), 32),
			new Key(String.valueOf((char) 0x37), new KeyTableEntry(KeyTableEntry.SEVEN, false, true), 32),
			new Key(String.valueOf((char) 0x38), new KeyTableEntry(KeyTableEntry.EIGHT, false, true), 32),
			new Key(String.valueOf((char) 0x39), new KeyTableEntry(KeyTableEntry.NINE, false, true), 32),
			new Key(String.valueOf((char) 0x30), new KeyTableEntry(KeyTableEntry.ZERO, false, true), 32),
			new Key(String.valueOf((char) 0xE6), new KeyTableEntry(KeyTableEntry.PLUS, false, true), 32),
			new Key(String.valueOf((char) 0xDC), new KeyTableEntry(KeyTableEntry.MINUS, false, true), 32),
			new Key(String.valueOf((char) 0xa8), new KeyTableEntry(KeyTableEntry.POUND, false, true), 32),
			new Key("CLR", new KeyTableEntry(KeyTableEntry.CLEAR_HOME, false, true), 56),
			new Key("INS", new KeyTableEntry(KeyTableEntry.INS_DEL, false, true), 56),
			new Key("F1", KeyTableEntry.F1, 56),
			};

	protected Key[] secondRow = new Key[] {
			new Key("CTRL", new KeyTableEntry(KeyTableEntry.CTRL, false, true), 60),
			new Key(String.valueOf((char) 0xEB), new KeyTableEntry(KeyTableEntry.Q, false, true), 32),
			new Key(String.valueOf((char) 0xF3), new KeyTableEntry(KeyTableEntry.W, false, true), 32),
			new Key(String.valueOf((char) 0xF2), new KeyTableEntry(KeyTableEntry.E, false, true), 32),
			new Key(String.valueOf((char) 0xF1), new KeyTableEntry(KeyTableEntry.R, false, true), 32),
			new Key(String.valueOf((char) 0xE3), new KeyTableEntry(KeyTableEntry.T, false, true), 32),
			new Key(String.valueOf((char) 0xF7), new KeyTableEntry(KeyTableEntry.Y, false, true), 32),
			new Key(String.valueOf((char) 0xF8), new KeyTableEntry(KeyTableEntry.U, false, true), 32),
			new Key(String.valueOf((char) 0xE2), new KeyTableEntry(KeyTableEntry.I, false, true), 32),
			new Key(String.valueOf((char) 0xF9), new KeyTableEntry(KeyTableEntry.O, false, true), 32),
			new Key(String.valueOf((char) 0xEF), new KeyTableEntry(KeyTableEntry.P, false, true), 32),
			new Key(String.valueOf((char) 0xE4), new KeyTableEntry(KeyTableEntry.AT, false, true), 32),
			new Key(String.valueOf((char) 0xDF), new KeyTableEntry(KeyTableEntry.STAR, false, true), 32),
			new Key(String.valueOf((char) 0xde), new KeyTableEntry(KeyTableEntry.ARROW_UP, false, true), 32),
			new Key("RESTORE", new KeyTableEntry(KeyTableEntry.RESTORE, false, true), 92),
			new Key("F3", KeyTableEntry.F3, 56),
	};

	protected Key[] thirdRow = new Key[] {
			new Key("RUN", new KeyTableEntry(KeyTableEntry.RUN_STOP, false, true), 50),
			new Key("STL", new KeyTableEntry(KeyTableEntry.SHIFT_LEFT, false, true), 50),
			new Key(String.valueOf((char) 0xF0), new KeyTableEntry(KeyTableEntry.A, false, true), 32),
			new Key(String.valueOf((char) 0xEE), new KeyTableEntry(KeyTableEntry.S, false, true), 32),
			new Key(String.valueOf((char) 0xEC), new KeyTableEntry(KeyTableEntry.D, false, true), 32),
			new Key(String.valueOf((char) 0xFB), new KeyTableEntry(KeyTableEntry.F, false, true), 32),
			new Key(String.valueOf((char) 0xE5), new KeyTableEntry(KeyTableEntry.G, false, true), 32),
			new Key(String.valueOf((char) 0xF4), new KeyTableEntry(KeyTableEntry.H, false, true), 32),
			new Key(String.valueOf((char) 0xF5), new KeyTableEntry(KeyTableEntry.J, false, true), 32),
			new Key(String.valueOf((char) 0xE1), new KeyTableEntry(KeyTableEntry.K, false, true), 32),
			new Key(String.valueOf((char) 0xF6), new KeyTableEntry(KeyTableEntry.L, false, true), 32),
			new Key(String.valueOf((char) 0x28), new KeyTableEntry(KeyTableEntry.COLON, false, true), 32),
			new Key(String.valueOf((char) 0x29), new KeyTableEntry(KeyTableEntry.SEMICOLON, false, true), 32),
			new Key(String.valueOf((char) 0x3d), new KeyTableEntry(KeyTableEntry.EQUALS, false, true), 32),
			new Key("RETRN", new KeyTableEntry(KeyTableEntry.RETURN, false, true), 82),
			new Key("F5", KeyTableEntry.F1, 56),
			};

	protected Key[] fourthRow = new Key[] {
			new Key("CMD", new KeyTableEntry(KeyTableEntry.COMMODORE, false, true), 50),
			new Key("SHL", new KeyTableEntry(KeyTableEntry.SHIFT_LEFT, false, true), 50), // XX shift-lock
			new Key(String.valueOf((char) 0xED), new KeyTableEntry(KeyTableEntry.Z, false, true), 32),
			new Key(String.valueOf((char) 0xFD), new KeyTableEntry(KeyTableEntry.X, false, true), 32),
			new Key(String.valueOf((char) 0xBC), new KeyTableEntry(KeyTableEntry.C, false, true), 32),
			new Key(String.valueOf((char) 0xFE), new KeyTableEntry(KeyTableEntry.V, false, true), 32),
			new Key(String.valueOf((char) 0xBF), new KeyTableEntry(KeyTableEntry.B, false, true), 32),
			new Key(String.valueOf((char) 0xE7), new KeyTableEntry(KeyTableEntry.N, false, true), 32),
			new Key(String.valueOf((char) 0xEA), new KeyTableEntry(KeyTableEntry.M, false, true), 32),
			new Key(String.valueOf((char) 0x3c), new KeyTableEntry(KeyTableEntry.COMMA, false, true), 32),
			new Key(String.valueOf((char) 0x3e), new KeyTableEntry(KeyTableEntry.PERIOD, false, true), 32),
			new Key(String.valueOf((char) 0x3f), new KeyTableEntry(KeyTableEntry.SLASH, false, true), 32),
			new Key("SHR", new KeyTableEntry(KeyTableEntry.SHIFT_RIGHT, false, true), 50),
			new Key("UP", new KeyTableEntry(KeyTableEntry.CURSOR_UP_DOWN, false, true), 50),
			new Key("LFT", new KeyTableEntry(KeyTableEntry.CURSOR_LEFT_RIGHT, false, true), 50),
			new Key("F7", KeyTableEntry.F7, 56),
	};

	protected Key[] fifthRow = new Key[] {
			new Key(String.valueOf((char) 0x20), new KeyTableEntry(KeyTableEntry.SPACE, false, true), 330),
	};

	/**
	 * @return the firstRow
	 */
	protected Key[] getFirstRow() {
		return firstRow;
	}

	/**
	 * @return the secondRow
	 */
	protected Key[] getSecondRow() {
		return secondRow;
	}

	/**
	 * @return the thirdRow
	 */
	protected Key[] getThirdRow() {
		return thirdRow;
	}

	/**
	 * @return the fourthRow
	 */
	protected Key[] getFourthRow() {
		return fourthRow;
	}

	/**
	 * @return the fifthRow
	 */
	protected Key[] getFifthRow() {
		return fifthRow;
	}

	protected String getLayoutName() {
		return "Commodore";
	}

}
