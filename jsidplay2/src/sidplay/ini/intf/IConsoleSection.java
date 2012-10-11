package sidplay.ini.intf;

public interface IConsoleSection {

	/**
	 * Getter of the top left character of the console output.
	 * 
	 * @return the top left character of the console output
	 */
	public char getTopLeft();

	/**
	 * Getter of the top right character of the console output.
	 * 
	 * @return the top right character of the console output
	 */
	public char getTopRight();

	/**
	 * Getter of the bottom left character of the console output.
	 * 
	 * @return the bottom left character of the console output
	 */
	public char getBottomLeft();

	/**
	 * Getter of the bottom right character of the console output.
	 * 
	 * @return the bottom right character of the console output
	 */
	public char getBottomRight();

	/**
	 * Getter of the vertical character of the console output.
	 * 
	 * @return the vertical character of the console output
	 */
	public char getVertical();

	/**
	 * Getter of the horizontal character of the console output.
	 * 
	 * @return the horizontal character of the console output
	 */
	public char getHorizontal();

	/**
	 * Getter of the junction left character of the console output.
	 * 
	 * @return the junction left character of the console output
	 */
	public char getJunctionLeft();

	/**
	 * Getter of the junction right character of the console output.
	 * 
	 * @return the junction right character of the console output
	 */
	public char getJunctionRight();

}