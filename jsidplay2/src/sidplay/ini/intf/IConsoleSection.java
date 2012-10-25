package sidplay.ini.intf;

public interface IConsoleSection {

	/**
	 * Getter of the top left character of the console output.
	 * 
	 * @return the top left character of the console output
	 */
	public char getTopLeft();

	public void setTopLeft(char topLeft);

	/**
	 * Getter of the top right character of the console output.
	 * 
	 * @return the top right character of the console output
	 */
	public char getTopRight();

	public void setTopRight(char topRight);

	/**
	 * Getter of the bottom left character of the console output.
	 * 
	 * @return the bottom left character of the console output
	 */
	public char getBottomLeft();

	public void setBottomLeft(char bottomLeft);

	/**
	 * Getter of the bottom right character of the console output.
	 * 
	 * @return the bottom right character of the console output
	 */
	public char getBottomRight();

	public void setBottomRight(char bottomRight);

	/**
	 * Getter of the vertical character of the console output.
	 * 
	 * @return the vertical character of the console output
	 */
	public char getVertical();

	public void setVertical(char vertical);

	/**
	 * Getter of the horizontal character of the console output.
	 * 
	 * @return the horizontal character of the console output
	 */
	public char getHorizontal();

	public void setHorizontal(char horizontal);

	/**
	 * Getter of the junction left character of the console output.
	 * 
	 * @return the junction left character of the console output
	 */
	public char getJunctionLeft();

	public void setJunctionLeft(char junctionLeft);

	/**
	 * Getter of the junction right character of the console output.
	 * 
	 * @return the junction right character of the console output
	 */
	public char getJunctionRight();

	public void setJunctionRight(char junctionRight);

}