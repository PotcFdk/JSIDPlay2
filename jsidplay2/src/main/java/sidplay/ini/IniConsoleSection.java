package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_CHAR_BOTTOM_LEFT;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_BOTTOM_RIGHT;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_HORIZONTAL;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_JUNCTION_LEFT;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_JUNCTION_RIGHT;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_TOP_LEFT;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_TOP_RIGHT;
import static sidplay.ini.IniDefaults.DEFAULT_CHAR_VERTICAL;

import sidplay.ini.converter.BeanToStringConverter;

/**
 * Console section of the INI file.
 *
 * @author Ken Händel
 *
 */
public class IniConsoleSection extends IniSection {

	private static final String SECTION_ID = "Console";

	protected IniConsoleSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Getter of the top left character of the console output.
	 *
	 * @return the top left character of the console output
	 */

	public final char getTopLeft() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Top Left", DEFAULT_CHAR_TOP_LEFT);
	}

	public final void setTopLeft(char topLeft) {
		iniReader.setProperty(SECTION_ID, "Char Top Left", topLeft);
	}

	/**
	 * Getter of the top right character of the console output.
	 *
	 * @return the top right character of the console output
	 */

	public final char getTopRight() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Top Right", DEFAULT_CHAR_TOP_RIGHT);
	}

	public final void setTopRight(char topRight) {
		iniReader.setProperty(SECTION_ID, "Char Top Right", topRight);
	}

	/**
	 * Getter of the bottom left character of the console output.
	 *
	 * @return the bottom left character of the console output
	 */

	public final char getBottomLeft() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Bottom Left", DEFAULT_CHAR_BOTTOM_LEFT);
	}

	public final void setBottomLeft(char bottomLeft) {
		iniReader.setProperty(SECTION_ID, "Char Bottom Left", bottomLeft);
	}

	/**
	 * Getter of the bottom right character of the console output.
	 *
	 * @return the bottom right character of the console output
	 */

	public final char getBottomRight() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Bottom Right", DEFAULT_CHAR_BOTTOM_RIGHT);
	}

	public final void setBottomRight(char bottomRight) {
		iniReader.setProperty(SECTION_ID, "Char Bottom Right", bottomRight);
	}

	/**
	 * Getter of the vertical character of the console output.
	 *
	 * @return the vertical character of the console output
	 */

	public final char getVertical() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Vertical", DEFAULT_CHAR_VERTICAL);
	}

	public final void setVertical(char vertical) {
		iniReader.setProperty(SECTION_ID, "Char Vertical", vertical);
	}

	/**
	 * Getter of the horizontal character of the console output.
	 *
	 * @return the horizontal character of the console output
	 */

	public final char getHorizontal() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Horizontal", DEFAULT_CHAR_HORIZONTAL);
	}

	public final void setHorizontal(char horizontal) {
		iniReader.setProperty(SECTION_ID, "Char Horizontal", horizontal);
	}

	/**
	 * Getter of the junction left character of the console output.
	 *
	 * @return the junction left character of the console output
	 */

	public final char getJunctionLeft() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Junction Left", DEFAULT_CHAR_JUNCTION_LEFT);
	}

	public final void setJunctionLeft(char junctionLeft) {
		iniReader.setProperty(SECTION_ID, "Char Junction Left", junctionLeft);
	}

	/**
	 * Getter of the junction right character of the console output.
	 *
	 * @return the junction right character of the console output
	 */

	public final char getJunctionRight() {
		return iniReader.getPropertyChar(SECTION_ID, "Char Junction Right", DEFAULT_CHAR_JUNCTION_RIGHT);
	}

	public final void setJunctionRight(char junctionRight) {
		iniReader.setProperty(SECTION_ID, "Char Junction Right", junctionRight);
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}

}