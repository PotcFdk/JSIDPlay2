package sidplay.ini;

import static sidplay.ini.IniDefaults.*;

/**
 * Console section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniConsoleSection extends IniSection {
	protected IniConsoleSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Getter of the top left character of the console output.
	 * 
	 * @return the top left character of the console output
	 */

	public final char getTopLeft() {
		return iniReader.getPropertyChar("Console", "Char Top Left", DEFAULT_CHAR_TOP_LEFT);
	}

	public void setTopLeft(char topLeft) {
		iniReader.setProperty("Console", "Char Top Left", topLeft);
	}

	/**
	 * Getter of the top right character of the console output.
	 * 
	 * @return the top right character of the console output
	 */

	public final char getTopRight() {
		return iniReader.getPropertyChar("Console", "Char Top Right", DEFAULT_CHAR_TOP_RIGHT);
	}

	public void setTopRight(char topRight) {
		iniReader.setProperty("Console", "Char Top Right", topRight);
	}

	/**
	 * Getter of the bottom left character of the console output.
	 * 
	 * @return the bottom left character of the console output
	 */

	public final char getBottomLeft() {
		return iniReader.getPropertyChar("Console", "Char Bottom Left", DEFAULT_CHAR_BOTTOM_LEFT);
	}

	public void setBottomLeft(char bottomLeft) {
		iniReader.setProperty("Console", "Char Bottom Left", bottomLeft);
	}

	/**
	 * Getter of the bottom right character of the console output.
	 * 
	 * @return the bottom right character of the console output
	 */

	public final char getBottomRight() {
		return iniReader.getPropertyChar("Console", "Char Bottom Right", DEFAULT_CHAR_BOTTOM_RIGHT);
	}

	public void setBottomRight(char bottomRight) {
		iniReader.setProperty("Console", "Char Bottom Right", bottomRight);
	}

	/**
	 * Getter of the vertical character of the console output.
	 * 
	 * @return the vertical character of the console output
	 */

	public final char getVertical() {
		return iniReader.getPropertyChar("Console", "Char Vertical", DEFAULT_CHAR_VERTICAL);
	}

	public void setVertical(char vertical) {
		iniReader.setProperty("Console", "Char Vertical", vertical);
	}

	/**
	 * Getter of the horizontal character of the console output.
	 * 
	 * @return the horizontal character of the console output
	 */

	public final char getHorizontal() {
		return iniReader.getPropertyChar("Console", "Char Horizontal", DEFAULT_CHAR_HORIZONTAL);
	}

	public void setHorizontal(char horizontal) {
		iniReader.setProperty("Console", "Char Horizontal", horizontal);
	}

	/**
	 * Getter of the junction left character of the console output.
	 * 
	 * @return the junction left character of the console output
	 */

	public final char getJunctionLeft() {
		return iniReader.getPropertyChar("Console", "Char Junction Left", DEFAULT_CHAR_JUNCTION_LEFT);
	}

	public void setJunctionLeft(char junctionLeft) {
		iniReader.setProperty("Console", "Char Junction Left", junctionLeft);
	}

	/**
	 * Getter of the junction right character of the console output.
	 * 
	 * @return the junction right character of the console output
	 */

	public final char getJunctionRight() {
		return iniReader.getPropertyChar("Console", "Char Junction Right", DEFAULT_CHAR_JUNCTION_RIGHT);
	}

	public void setJunctionRight(char junctionRight) {
		iniReader.setProperty("Console", "Char Junction Right", junctionRight);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("topLeft=").append(getTopLeft()).append(",");
		result.append("topRight=").append(getTopRight()).append(",");
		result.append("bottomLeft=").append(getBottomLeft()).append(",");
		result.append("bottomRight=").append(getBottomRight()).append(",");
		result.append("vertical=").append(getVertical()).append(",");
		result.append("horizontal=").append(getHorizontal()).append(",");
		result.append("junctionLeft=").append(getJunctionLeft()).append(",");
		result.append("junctionRight=").append(getJunctionRight());
		return result.toString();
	}
}