package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IConsoleSection;

@Embeddable
public class DbConsoleSection implements IConsoleSection {

	private char topLeft;

	public void setTopLeft(char topLeft) {
		this.topLeft = topLeft;
	}

	@Override
	public char getTopLeft() {
		return this.topLeft;
	}

	private char topRight;

	public void setTopRight(char topRight) {
		this.topRight = topRight;
	}

	@Override
	public char getTopRight() {
		return this.topRight;
	}

	private char bottomLeft;

	public void setBottomLeft(char bottomLeft) {
		this.bottomLeft = bottomLeft;
	}

	@Override
	public char getBottomLeft() {
		return this.bottomLeft;
	}

	private char bottomRight;

	public void setBottomRight(char bottomRight) {
		this.bottomRight = bottomRight;
	}

	@Override
	public char getBottomRight() {
		return this.bottomRight;
	}

	private char vertical;

	public void setVertical(char vertical) {
		this.vertical = vertical;
	}

	@Override
	public char getVertical() {
		return this.vertical;
	}

	private char horizontal;

	public void setHorizontal(char horizontal) {
		this.horizontal = horizontal;
	}

	@Override
	public char getHorizontal() {
		return this.horizontal;
	}

	private char junctionLeft;

	public void setJunctionLeft(char junctionLeft) {
		this.junctionLeft = junctionLeft;
	}

	@Override
	public char getJunctionLeft() {
		return junctionLeft;
	}

	private char junctionRight;

	public void setJunctionRight(char junctionRight) {
		this.junctionRight = junctionRight;
	}

	@Override
	public char getJunctionRight() {
		return this.junctionRight;
	}

}
