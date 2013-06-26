package libsidplay.components.keyboard;

/**
 * Data structure for an entry on the key table of the C64 keyboard
 */
public enum KeyTableEntry {
	ARROW_LEFT(7, 1), ONE(7, 0), TWO(7, 3), THREE(1, 0), FOUR(1, 3), FIVE(2, 0), SIX(
			2, 3), SEVEN(3, 0), EIGHT(3, 3), NINE(4, 0), ZERO(4, 3), PLUS(5, 0), MINUS(
			5, 3), POUND(6, 0), CLEAR_HOME(6, 3), INS_DEL(0, 0), CTRL(7, 2), Q(
			7, 6), W(1, 1), E(1, 6), R(2, 1), T(2, 6), Y(3, 1), U(3, 6), I(4, 1), O(
			4, 6), P(5, 1), AT(5, 6), STAR(6, 1), ARROW_UP(6, 6), RUN_STOP(7, 7), A(
			1, 2), S(1, 5), D(2, 2), F(2, 5), G(3, 2), H(3, 5), J(4, 2), K(4, 5), L(
			5, 2), COLON(5, 5), SEMICOLON(6, 2), EQUALS(6, 5), RETURN(0, 1), COMMODORE(
			7, 5), SHIFT_LEFT(1, 7), Z(1, 4), X(2, 7), C(2, 4), V(3, 7), B(3, 4), N(
			4, 7), M(4, 4), COMMA(5, 7), PERIOD(5, 4), SLASH(6, 7), SHIFT_RIGHT(
			6, 4), CURSOR_UP_DOWN(0, 7), CURSOR_LEFT_RIGHT(0, 2), SPACE(7, 4), F1(
			0, 4), F3(0, 5), F5(0, 6), F7(0, 3), RESTORE(-1, -1);

	private final int row, col;

	private KeyTableEntry(final int row, final int col) {
		this.row = row;
		this.col = col;
	}

	@Override
	public final String toString() {
		return this.getClass().getName() + "( " + this.getRow() + ", "
				+ this.getCol() + " )";
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

}