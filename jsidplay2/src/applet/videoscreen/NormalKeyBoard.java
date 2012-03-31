package applet.videoscreen;

import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JSlider;

import libsidplay.C64;
import libsidplay.components.keyboard.KeyTableEntry;

public class NormalKeyBoard extends VirtualKeyboard {

	private Key[] firstRow = new Key[] {
			new Key(String.valueOf((char) 0x5f), KeyTableEntry.ARROW_LEFT, 32),
			new Key(String.valueOf((char) 0x31), KeyTableEntry.ONE, 32),
			new Key(String.valueOf((char) 0x32), KeyTableEntry.TWO, 32),
			new Key(String.valueOf((char) 0x33), KeyTableEntry.THREE, 32),
			new Key(String.valueOf((char) 0x34), KeyTableEntry.FOUR, 32),
			new Key(String.valueOf((char) 0x35), KeyTableEntry.FIVE, 32),
			new Key(String.valueOf((char) 0x36), KeyTableEntry.SIX, 32),
			new Key(String.valueOf((char) 0x37), KeyTableEntry.SEVEN, 32),
			new Key(String.valueOf((char) 0x38), KeyTableEntry.EIGHT, 32),
			new Key(String.valueOf((char) 0x39), KeyTableEntry.NINE, 32),
			new Key(String.valueOf((char) 0x30), KeyTableEntry.ZERO, 32),
			new Key(String.valueOf((char) 0x2b), KeyTableEntry.PLUS, 32),
			new Key(String.valueOf((char) 0x2d), KeyTableEntry.MINUS, 32),
			new Key(String.valueOf((char) 0x5c), KeyTableEntry.POUND, 32),
			new Key("HOM", KeyTableEntry.CLEAR_HOME, 56),
			new Key("DEL", KeyTableEntry.INS_DEL, 56),
			new Key("F1", KeyTableEntry.F1, 56),
			};
	private JButton[] firstRowButtons = new JButton[firstRow.length];

	private Key[] secondRow = new Key[] {
			new Key("CTRL", KeyTableEntry.CTRL, 60),
			new Key(String.valueOf((char) 0x51), KeyTableEntry.Q, 32),
			new Key(String.valueOf((char) 0x57), KeyTableEntry.W, 32),
			new Key(String.valueOf((char) 0x45), KeyTableEntry.E, 32),
			new Key(String.valueOf((char) 0x52), KeyTableEntry.R, 32),
			new Key(String.valueOf((char) 0x54), KeyTableEntry.T, 32),
			new Key(String.valueOf((char) 0x59), KeyTableEntry.Y, 32),
			new Key(String.valueOf((char) 0x55), KeyTableEntry.U, 32),
			new Key(String.valueOf((char) 0x49), KeyTableEntry.I, 32),
			new Key(String.valueOf((char) 0x4f), KeyTableEntry.O, 32),
			new Key(String.valueOf((char) 0x50), KeyTableEntry.P, 32),
			new Key(String.valueOf((char) 0x40), KeyTableEntry.AT, 32),
			new Key(String.valueOf((char) 0x2a), KeyTableEntry.STAR, 32),
			new Key(String.valueOf((char) 0x5e), KeyTableEntry.ARROW_UP, 32),
			new Key("RESTORE", KeyTableEntry.RESTORE, 92),
			new Key("F3", KeyTableEntry.F3, 56),
	};
	private JButton[] secondRowButtons = new JButton[secondRow.length];

	private Key[] thirdRow = new Key[] {
			new Key("STP", KeyTableEntry.RUN_STOP, 50),
			new Key("STL", KeyTableEntry.SHIFT_LEFT, 50),
			new Key(String.valueOf((char) 0x41), KeyTableEntry.A, 32),
			new Key(String.valueOf((char) 0x53), KeyTableEntry.S, 32),
			new Key(String.valueOf((char) 0x44), KeyTableEntry.D, 32),
			new Key(String.valueOf((char) 0x46), KeyTableEntry.F, 32),
			new Key(String.valueOf((char) 0x47), KeyTableEntry.G, 32),
			new Key(String.valueOf((char) 0x48), KeyTableEntry.H, 32),
			new Key(String.valueOf((char) 0x4a), KeyTableEntry.J, 32),
			new Key(String.valueOf((char) 0x4b), KeyTableEntry.K, 32),
			new Key(String.valueOf((char) 0x4c), KeyTableEntry.L, 32),
			new Key(String.valueOf((char) 0x3a), KeyTableEntry.COLON, 32),
			new Key(String.valueOf((char) 0x3b), KeyTableEntry.SEMICOLON, 32),
			new Key(String.valueOf((char) 0x3d), KeyTableEntry.EQUALS, 32),
			new Key("RETRN", KeyTableEntry.RETURN, 82),
			new Key("F5", KeyTableEntry.F5, 56),
			};
	private JButton[] thirdRowButtons = new JButton[thirdRow.length];

	private Key[] fourthRow = new Key[] {
			new Key("CMD", KeyTableEntry.COMMODORE, 50),
			new Key("SHL", KeyTableEntry.SHIFT_LEFT, 50), // XX shift-lock
			new Key(String.valueOf((char) 0x5a), KeyTableEntry.Z, 32),
			new Key(String.valueOf((char) 0x58), KeyTableEntry.X, 32),
			new Key(String.valueOf((char) 0x43), KeyTableEntry.C, 32),
			new Key(String.valueOf((char) 0x56), KeyTableEntry.V, 32),
			new Key(String.valueOf((char) 0x42), KeyTableEntry.B, 32),
			new Key(String.valueOf((char) 0x4e), KeyTableEntry.N, 32),
			new Key(String.valueOf((char) 0x4d), KeyTableEntry.M, 32),
			new Key(String.valueOf((char) 0x2c), KeyTableEntry.COMMA, 32),
			new Key(String.valueOf((char) 0x2e), KeyTableEntry.PERIOD, 32),
			new Key(String.valueOf((char) 0x2f), KeyTableEntry.SLASH, 32),
			new Key("SHR", KeyTableEntry.SHIFT_RIGHT, 50),
			new Key("DWN", KeyTableEntry.CURSOR_UP_DOWN, 50),
			new Key("RGT", KeyTableEntry.CURSOR_LEFT_RIGHT, 50),
			new Key("F7", KeyTableEntry.F7, 56),
			};
	private JButton[] fourthRowButtons = new JButton[fourthRow.length];

	private Key[] fifthRow = new Key[] { new Key(String.valueOf((char) 0x20),
			KeyTableEntry.SPACE, 330), };
	private JButton[] fifthRowButtons = new JButton[fifthRow.length];

	public Rectangle createUI(Container parent, C64 c64, JSlider slider) {
		int fctnMargin = 8;
		int posX = 0, posY = slider.getMinimumSize().height, maxX = 0, maxY = 0;
		final int height = 32;
		for (int i = 0; i < getFirstRow().length; i++) {
			final KeyTableEntry key = getFirstRow()[i].getEntry();
			final int width = getFirstRow()[i].getWidth();
			firstRowButtons[i] = createKey(c64, posX, posY, width, height,
					getFirstRow()[i].getPetscii(), key, slider);
			posX = posX + width + 4;
			if (i == getFirstRow().length - 2) {
				posX += fctnMargin;
			}
			maxX = Math.max(posX, maxX);
			maxY = Math.max(posY, maxY);
			parent.add(firstRowButtons[i]);
		}

		posX = 0;
		posY += height + 4;
		for (int i = 0; i < getSecondRow().length; i++) {
			final KeyTableEntry key = getSecondRow()[i].getEntry();
			final int width = getSecondRow()[i].getWidth();
			secondRowButtons[i] = createKey(c64, posX, posY, width, height,
					getSecondRow()[i].getPetscii(), key, slider);
			posX = posX + width + 4;
			if (i == getSecondRow().length - 2) {
				posX += fctnMargin;
			}
			maxX = Math.max(posX, maxX);
			maxY = Math.max(posY, maxY);
			parent.add(secondRowButtons[i]);
		}

		posX = 0;
		posY += height + 4;
		for (int i = 0; i < getThirdRow().length; i++) {
			final KeyTableEntry key = getThirdRow()[i].getEntry();
			final int width = getThirdRow()[i].getWidth();
			thirdRowButtons[i] = createKey(c64, posX, posY, width, height,
					getThirdRow()[i].getPetscii(), key, slider);
			posX = posX + width + 4;
			if (i == getThirdRow().length - 2) {
				posX += fctnMargin;
			}
			maxX = Math.max(posX, maxX);
			maxY = Math.max(posY, maxY);
			parent.add(thirdRowButtons[i]);
		}

		posX = 0;
		posY += height + 4;
		for (int i = 0; i < getFourthRow().length; i++) {
			final KeyTableEntry key = getFourthRow()[i].getEntry();
			final int width = getFourthRow()[i].getWidth();
			fourthRowButtons[i] = createKey(c64, posX, posY, width, height,
					getFourthRow()[i].getPetscii(), key, slider);
			posX = posX + width + 4;
			if (i == getFourthRow().length - 2) {
				posX += fctnMargin;
			}
			maxX = Math.max(posX, maxX);
			maxY = Math.max(posY, maxY);
			parent.add(fourthRowButtons[i]);
		}

		posX = 102;
		posY += height + 4;
		for (int i = 0; i < getFifthRow().length; i++) {
			final KeyTableEntry key = getFifthRow()[i].getEntry();
			final int width = getFifthRow()[i].getWidth();
			fifthRowButtons[i] = createKey(c64, posX, posY, width, height,
					getFifthRow()[i].getPetscii(), key, slider);
			posX = posX + width + 4;
			maxX = Math.max(posX, maxX);
			maxY = Math.max(posY, maxY);
			parent.add(fifthRowButtons[i]);
		}
		parent.setBounds(0, 0, maxX + 20, maxY + 80);
		return parent.getBounds();
	}

	protected String getLayoutName() {
		return "Normal";
	}

	public boolean isVisible() {
		return firstRowButtons[0].isVisible();
	}

	public void setVisible(boolean b) {
		for (final JButton row : firstRowButtons) {
			row.setVisible(b);
		}
		for (final JButton row : secondRowButtons) {
			row.setVisible(b);
		}
		for (final JButton row : thirdRowButtons) {
			row.setVisible(b);
		}
		for (final JButton row : fourthRowButtons) {
			row.setVisible(b);
		}
		for (final JButton row : fifthRowButtons) {
			row.setVisible(b);
		}
	}

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

}
