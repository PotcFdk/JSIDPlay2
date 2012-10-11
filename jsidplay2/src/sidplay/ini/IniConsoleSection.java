package sidplay.ini;

import sidplay.ini.intf.IConsoleSection;

/**
 * Console section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniConsoleSection extends IniSection implements IConsoleSection {
	protected IniConsoleSection(IniReader iniReader) {
		super(iniReader);
	}

	private static char getChar(final String str, final char defaultChar) {
		char c = 0;
		if (str.length() == 0) {
			return defaultChar;
		}
		// Check if we have an actual Character
		if (str.charAt(0) == '\'') {
			if (str.charAt(2) != '\'') {
				throw new RuntimeException("Invalid character notation: " + str);
			} else {
				c = str.charAt(1);
			}
		} // Nope is number
		else {
			c = (char) Integer.parseInt(str);
		}

		// Clip off special characters
		if (c >= 32) {
			return c;
		}
		return defaultChar;
	}

	/**
	 * Getter of the top left character of the console output.
	 * 
	 * @return the top left character of the console output
	 */
	@Override
	public final char getTopLeft() {
		return getChar(
				iniReader.getPropertyString("Console", "Char Top Left", "'+'"),
				'+');
	}

	/**
	 * Getter of the top right character of the console output.
	 * 
	 * @return the top right character of the console output
	 */
	@Override
	public final char getTopRight() {
		return getChar(
				iniReader.getPropertyString("Console", "Char Top Right", "'+'"),
				'+');
	}

	/**
	 * Getter of the bottom left character of the console output.
	 * 
	 * @return the bottom left character of the console output
	 */
	@Override
	public final char getBottomLeft() {
		return getChar(iniReader.getPropertyString("Console",
				"Char Bottom Left", "'+'"), '+');
	}

	/**
	 * Getter of the bottom right character of the console output.
	 * 
	 * @return the bottom right character of the console output
	 */
	@Override
	public final char getBottomRight() {
		return getChar(iniReader.getPropertyString("Console",
				"Char Bottom Right", "'+'"), '+');
	}

	/**
	 * Getter of the vertical character of the console output.
	 * 
	 * @return the vertical character of the console output
	 */
	@Override
	public final char getVertical() {
		return getChar(
				iniReader.getPropertyString("Console", "Char Vertical", "'|'"),
				'|');
	}

	/**
	 * Getter of the horizontal character of the console output.
	 * 
	 * @return the horizontal character of the console output
	 */
	@Override
	public final char getHorizontal() {
		return getChar(
				iniReader.getPropertyString("Console", "Char Vertical", "'-'"),
				'-');
	}

	/**
	 * Getter of the junction left character of the console output.
	 * 
	 * @return the junction left character of the console output
	 */
	@Override
	public final char getJunctionLeft() {
		return getChar(iniReader.getPropertyString("Console",
				"Char Junction Left", "'-'"), '-');
	}

	/**
	 * Getter of the junction right character of the console output.
	 * 
	 * @return the junction right character of the console output
	 */
	@Override
	public final char getJunctionRight() {
		return getChar(iniReader.getPropertyString("Console",
				"Char Junction Right", "'-'"), '-');
	}
}