package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IConsoleSection;
import applet.config.annotations.ConfigDescription;

@Embeddable
public class ConsoleSection implements IConsoleSection {

	@ConfigDescription(bundleKey = "CONSOLE_TOP_LEFT_DESC", toolTipBundleKey = "CONSOLE_TOP_LEFT_TOOLTIP")
	private char topLeft = '+';

	@Override
	public void setTopLeft(char topLeft) {
		this.topLeft = topLeft;
	}

	@Override
	public char getTopLeft() {
		return this.topLeft;
	}

	@ConfigDescription(bundleKey = "CONSOLE_TOP_RIGHT_DESC", toolTipBundleKey = "CONSOLE_TOP_RIGHT_TOOLTIP")
	private char topRight = '+';

	@Override
	public void setTopRight(char topRight) {
		this.topRight = topRight;
	}

	@Override
	public char getTopRight() {
		return this.topRight;
	}

	@ConfigDescription(bundleKey = "CONSOLE_BOTTOM_LEFT_DESC", toolTipBundleKey = "CONSOLE_BOTTOM_LEFT_TOOLTIP")
	private char bottomLeft = '+';

	@Override
	public void setBottomLeft(char bottomLeft) {
		this.bottomLeft = bottomLeft;
	}

	@Override
	public char getBottomLeft() {
		return this.bottomLeft;
	}

	@ConfigDescription(bundleKey = "CONSOLE_BOTTOM_RIGHT_DESC", toolTipBundleKey = "CONSOLE_BOTTOM_RIGHT_TOOLTIP")
	private char bottomRight = '+';

	@Override
	public void setBottomRight(char bottomRight) {
		this.bottomRight = bottomRight;
	}

	@Override
	public char getBottomRight() {
		return this.bottomRight;
	}

	@ConfigDescription(bundleKey = "CONSOLE_VERTICAL_DESC", toolTipBundleKey = "CONSOLE_VERTICAL_TOOLTIP")
	private char vertical = '|';

	@Override
	public void setVertical(char vertical) {
		this.vertical = vertical;
	}

	@Override
	public char getVertical() {
		return this.vertical;
	}

	@ConfigDescription(bundleKey = "CONSOLE_HORIZONTAL_DESC", toolTipBundleKey = "CONSOLE_HORIZONTAL_TOOLTIP")
	private char horizontal = '-';

	@Override
	public void setHorizontal(char horizontal) {
		this.horizontal = horizontal;
	}

	@Override
	public char getHorizontal() {
		return this.horizontal;
	}

	@ConfigDescription(bundleKey = "CONSOLE_JUNCTION_LEFT_DESC", toolTipBundleKey = "CONSOLE_JUNCTION_LEFT_TOOLTIP")
	private char junctionLeft = '+';

	@Override
	public void setJunctionLeft(char junctionLeft) {
		this.junctionLeft = junctionLeft;
	}

	@Override
	public char getJunctionLeft() {
		return junctionLeft;
	}

	@ConfigDescription(bundleKey = "CONSOLE_JUNCTION_RIGHT_DESC", toolTipBundleKey = "CONSOLE_JUNCTION_RIGHT_TOOLTIP")
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
