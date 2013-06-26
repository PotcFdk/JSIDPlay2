package ui.joysticksettings;

import libsidplay.components.joystick.IJoystick;
import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Implementation of a Joystick. Connects selected controller values with the
 * emulation core.
 */
final class JoystickReader implements IJoystick {
	/**
	 * Maximum accepted value difference for input component.
	 */
	private static final double DELTA_VALUE = 0.1;

	/**
	 * Polling delay.
	 */
	private static final int POLL_DELAY_IN_MS = 5;

	private Controller controller;
	private Component up, down, left, right, fire;

	private long lastPollingTime;
	private byte bits;
	private float upValue, downValue, leftValue, rightValue, fireValue;

	public JoystickReader(Controller controller, Component up, Component down,
			Component left, Component right, Component fire, float upValue,
			float downValue, float leftValue, float rightValue, float fireValue) {
		this.controller = controller;
		this.up = up;
		this.down = down;
		this.left = left;
		this.right = right;
		this.fire = fire;
		this.upValue = upValue;
		this.downValue = downValue;
		this.leftValue = leftValue;
		this.rightValue = rightValue;
		this.fireValue = fireValue;
	}

	@Override
	public byte getValue() {
		if (controller == null) {
			return (byte) 0xff;
		}
		/* throttle polling to max. once every 5 ms */
		final long currentTime = System.currentTimeMillis();
		if (currentTime > lastPollingTime + POLL_DELAY_IN_MS) {
			controller.poll();
			lastPollingTime = currentTime;

			bits = (byte) 0xff;
			if (up != null
					&& Math.abs(up.getPollData() - upValue) < DELTA_VALUE) {
				bits ^= 1;
			}
			if (down != null
					&& Math.abs(down.getPollData() - downValue) < DELTA_VALUE) {
				bits ^= 2;
			}
			if (left != null
					&& Math.abs(left.getPollData() - leftValue) < DELTA_VALUE) {
				bits ^= 4;
			}
			if (right != null
					&& Math.abs(right.getPollData() - rightValue) < DELTA_VALUE) {
				bits ^= 8;
			}
			if (fire != null
					&& Math.abs(fire.getPollData() - fireValue) < DELTA_VALUE) {
				bits ^= 16;
			}
		}
		return bits;
	}
}