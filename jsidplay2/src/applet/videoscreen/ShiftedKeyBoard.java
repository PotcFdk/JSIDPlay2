package applet.videoscreen;

import libsidplay.components.keyboard.KeyTableEntry;

public class ShiftedKeyBoard extends NormalKeyBoard {

	protected Key[] firstRow = new Key[] {
			new Key(String.valueOf((char) 0x5f), new KeyTableEntry(KeyTableEntry.ARROW_LEFT, true), 32),
			new Key(String.valueOf((char) 0x21), new KeyTableEntry(KeyTableEntry.ONE, true), 32),
			new Key(String.valueOf((char) 0x22), new KeyTableEntry(KeyTableEntry.TWO, true), 32),
			new Key(String.valueOf((char) 0x23), new KeyTableEntry(KeyTableEntry.THREE, true), 32),
			new Key(String.valueOf((char) 0x24), new KeyTableEntry(KeyTableEntry.FOUR, true), 32),
			new Key(String.valueOf((char) 0x25), new KeyTableEntry(KeyTableEntry.FIVE, true), 32),
			new Key(String.valueOf((char) 0x26), new KeyTableEntry(KeyTableEntry.SIX, true), 32),
			new Key(String.valueOf((char) 0x27), new KeyTableEntry(KeyTableEntry.SEVEN, true), 32),
			new Key(String.valueOf((char) 0x28), new KeyTableEntry(KeyTableEntry.EIGHT, true), 32),
			new Key(String.valueOf((char) 0x29), new KeyTableEntry(KeyTableEntry.NINE, true), 32),
			new Key(String.valueOf((char) 0x30), new KeyTableEntry(KeyTableEntry.ZERO, true), 32),
			new Key(String.valueOf((char) 0xDB), new KeyTableEntry(KeyTableEntry.PLUS, true), 32),
			new Key(String.valueOf((char) 0xDD), new KeyTableEntry(KeyTableEntry.MINUS, true), 32),
			new Key(String.valueOf((char) 0xa9), new KeyTableEntry(KeyTableEntry.POUND, true), 32),
			new Key("CLR", new KeyTableEntry(KeyTableEntry.CLEAR_HOME, true), 56),
			new Key("INS", new KeyTableEntry(KeyTableEntry.INS_DEL, true), 56),
			new Key("F2", new KeyTableEntry(KeyTableEntry.F1, true), 56),
			};

	protected Key[] secondRow = new Key[] {
			new Key("CTRL", new KeyTableEntry(KeyTableEntry.CTRL, true), 60),
			new Key(String.valueOf((char) 0x71), new KeyTableEntry(KeyTableEntry.Q, true), 32),
			new Key(String.valueOf((char) 0x77), new KeyTableEntry(KeyTableEntry.W, true), 32),
			new Key(String.valueOf((char) 0x65), new KeyTableEntry(KeyTableEntry.E, true), 32),
			new Key(String.valueOf((char) 0x72), new KeyTableEntry(KeyTableEntry.R, true), 32),
			new Key(String.valueOf((char) 0x74), new KeyTableEntry(KeyTableEntry.T, true), 32),
			new Key(String.valueOf((char) 0x79), new KeyTableEntry(KeyTableEntry.Y, true), 32),
			new Key(String.valueOf((char) 0x75), new KeyTableEntry(KeyTableEntry.U, true), 32),
			new Key(String.valueOf((char) 0x69), new KeyTableEntry(KeyTableEntry.I, true), 32),
			new Key(String.valueOf((char) 0x6f), new KeyTableEntry(KeyTableEntry.O, true), 32),
			new Key(String.valueOf((char) 0x70), new KeyTableEntry(KeyTableEntry.P, true), 32),
			new Key(String.valueOf((char) 0xba), new KeyTableEntry(KeyTableEntry.AT, true), 32),
			new Key(String.valueOf((char) 0xc6), new KeyTableEntry(KeyTableEntry.STAR, true), 32),
			new Key(String.valueOf((char) 0xde), new KeyTableEntry(KeyTableEntry.ARROW_UP, true), 32),
			new Key("RESTORE", new KeyTableEntry(KeyTableEntry.RESTORE, true), 92),
			new Key("F4", new KeyTableEntry(KeyTableEntry.F3, true), 56),
	};

	protected Key[] thirdRow = new Key[] {
			new Key("RUN", new KeyTableEntry(KeyTableEntry.RUN_STOP, true), 50),
			new Key("STL", new KeyTableEntry(KeyTableEntry.SHIFT_LEFT, true), 50),
			new Key(String.valueOf((char) 0x61), new KeyTableEntry(KeyTableEntry.A, true), 32),
			new Key(String.valueOf((char) 0x73), new KeyTableEntry(KeyTableEntry.S, true), 32),
			new Key(String.valueOf((char) 0x64), new KeyTableEntry(KeyTableEntry.D, true), 32),
			new Key(String.valueOf((char) 0x66), new KeyTableEntry(KeyTableEntry.F, true), 32),
			new Key(String.valueOf((char) 0x67), new KeyTableEntry(KeyTableEntry.G, true), 32),
			new Key(String.valueOf((char) 0x68), new KeyTableEntry(KeyTableEntry.H, true), 32),
			new Key(String.valueOf((char) 0x6a), new KeyTableEntry(KeyTableEntry.J, true), 32),
			new Key(String.valueOf((char) 0x6b), new KeyTableEntry(KeyTableEntry.K, true), 32),
			new Key(String.valueOf((char) 0x6c), new KeyTableEntry(KeyTableEntry.L, true), 32),
			new Key(String.valueOf((char) 0x28), new KeyTableEntry(KeyTableEntry.COLON, true), 32),
			new Key(String.valueOf((char) 0x29), new KeyTableEntry(KeyTableEntry.SEMICOLON, true), 32),
			new Key(String.valueOf((char) 0x3d), new KeyTableEntry(KeyTableEntry.EQUALS, true), 32),
			new Key("RETRN", new KeyTableEntry(KeyTableEntry.RETURN, true), 82),
			new Key("F6", new KeyTableEntry(KeyTableEntry.F5, true), 56),
			};

	protected Key[] fourthRow = new Key[] {
			new Key("CMD", new KeyTableEntry(KeyTableEntry.COMMODORE, true), 50),
			new Key("SHL", new KeyTableEntry(KeyTableEntry.SHIFT_LEFT, true), 50), // XX shift-lock
			new Key(String.valueOf((char) 0x7a), new KeyTableEntry(KeyTableEntry.Z, true), 32),
			new Key(String.valueOf((char) 0x78), new KeyTableEntry(KeyTableEntry.X, true), 32),
			new Key(String.valueOf((char) 0x63), new KeyTableEntry(KeyTableEntry.C, true), 32),
			new Key(String.valueOf((char) 0x76), new KeyTableEntry(KeyTableEntry.V, true), 32),
			new Key(String.valueOf((char) 0x62), new KeyTableEntry(KeyTableEntry.B, true), 32),
			new Key(String.valueOf((char) 0x6e), new KeyTableEntry(KeyTableEntry.N, true), 32),
			new Key(String.valueOf((char) 0x6d), new KeyTableEntry(KeyTableEntry.M, true), 32),
			new Key(String.valueOf((char) 0x3c), new KeyTableEntry(KeyTableEntry.COMMA, true), 32),
			new Key(String.valueOf((char) 0x3e), new KeyTableEntry(KeyTableEntry.PERIOD, true), 32),
			new Key(String.valueOf((char) 0x3f), new KeyTableEntry(KeyTableEntry.SLASH, true), 32),
			new Key("SHR", new KeyTableEntry(KeyTableEntry.SHIFT_RIGHT, true), 50),
			new Key("UP", new KeyTableEntry(KeyTableEntry.CURSOR_UP_DOWN, true), 50),
			new Key("LFT", new KeyTableEntry(KeyTableEntry.CURSOR_LEFT_RIGHT, true), 50),
			new Key("F8", new KeyTableEntry(KeyTableEntry.F7, true), 56),
	};

	protected Key[] fifthRow = new Key[] {
			new Key(String.valueOf((char) 0x20), new KeyTableEntry(KeyTableEntry.SPACE, true), 330),
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
		return "Shifted";
	}

}
