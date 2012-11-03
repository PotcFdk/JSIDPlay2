package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IConsoleSection;
import applet.config.annotations.ConfigDescription;

@Embeddable
public class ConsoleSection implements IConsoleSection {

	@ConfigDescription(descriptionKey = "CONSOLE_TOP_LEFT_DESC", toolTipKey = "CONSOLE_TOP_LEFT_TOOLTIP")
	private char topLeft = '+';

	@Override
	public void setTopLeft(char topLeft) {
		this.topLeft = topLeft;
	}

	@Override
	public char getTopLeft() {
		return this.topLeft;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_TOP_RIGHT_DESC", toolTipKey = "CONSOLE_TOP_RIGHT_TOOLTIP")
	private char topRight = '+';

	@Override
	public void setTopRight(char topRight) {
		this.topRight = topRight;
	}

	@Override
	public char getTopRight() {
		return this.topRight;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_BOTTOM_LEFT_DESC", toolTipKey = "CONSOLE_BOTTOM_LEFT_TOOLTIP")
	private char bottomLeft = '+';

	@Override
	public void setBottomLeft(char bottomLeft) {
		this.bottomLeft = bottomLeft;
	}

	@Override
	public char getBottomLeft() {
		return this.bottomLeft;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_BOTTOM_RIGHT_DESC", toolTipKey = "CONSOLE_BOTTOM_RIGHT_TOOLTIP")
	private char bottomRight = '+';

	@Override
	public void setBottomRight(char bottomRight) {
		this.bottomRight = bottomRight;
	}

	@Override
	public char getBottomRight() {
		return this.bottomRight;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_VERTICAL_DESC", toolTipKey = "CONSOLE_VERTICAL_TOOLTIP")
	private char vertical = '|';

	@Override
	public void setVertical(char vertical) {
		this.vertical = vertical;
	}

	@Override
	public char getVertical() {
		return this.vertical;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_HORIZONTAL_DESC", toolTipKey = "CONSOLE_HORIZONTAL_TOOLTIP")
	private char horizontal = '-';

	@Override
	public void setHorizontal(char horizontal) {
		this.horizontal = horizontal;
	}

	@Override
	public char getHorizontal() {
		return this.horizontal;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_JUNCTION_LEFT_DESC", toolTipKey = "CONSOLE_JUNCTION_LEFT_TOOLTIP")
	private char junctionLeft = '+';

	@Override
	public void setJunctionLeft(char junctionLeft) {
		this.junctionLeft = junctionLeft;
	}

	@Override
	public char getJunctionLeft() {
		return junctionLeft;
	}

	@ConfigDescription(descriptionKey = "CONSOLE_JUNCTION_RIGHT_DESC", toolTipKey = "CONSOLE_JUNCTION_RIGHT_TOOLTIP")
	private char junctionRight = '+';

	@Override
	public void setJunctionRight(char junctionRight) {
		this.junctionRight = junctionRight;
	}

	@Override
	public char getJunctionRight() {
		return this.junctionRight;
	}

}
