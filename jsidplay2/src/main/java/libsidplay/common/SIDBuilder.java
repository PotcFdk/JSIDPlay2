/**
 *                           Sid Builder Classes
 *                           -------------------
 *  begin                : Sat May 6 2001
 *  copyright            : (C) 2001 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package libsidplay.common;

import libsidplay.sidtune.SidTune;

/**
 * @author Ken Händel
 * 
 *         Implement this class to create a new SID emulations for libsidplay2.
 */
public interface SIDBuilder {
	/**
	 * Create a new SID chip emulation.
	 * 
	 * @param device
	 *            old SID chip in use
	 * @param sidNum
	 *            SID chip number
	 * @param tune
	 *            current tune
	 * 
	 * @return emulated SID chip
	 */
	SIDEmu lock(SIDEmu device, int sidNum, SidTune tune);

	/**
	 * Destroy SID chip emulation.
	 * 
	 * @param device
	 *            SID chip to destroy
	 */
	void unlock(SIDEmu device);

	/**
	 * @return current number of SID devices.
	 */
	int getSIDCount();

	/**
	 * Reset.
	 */
	void reset();

	/**
	 * Timer start reached, audio output should be produced.
	 */
	void start();

	/**
	 * Volume of the SID chip
	 * 
	 * @param sidNum
	 *            SID chip number
	 */
	void setVolume(int sidNum);

	/**
	 * Fade-in start time reached, audio volume should be increased to the max.
	 * 
	 * @param fadeIn
	 *            Fade-in time in seconds
	 */
	void fadeIn(int fadeIn);
	
	/**
	 * Fade-out start time reached, audio volume should be lowered to zero.
	 * 
	 * @param fadeOut
	 *            Fade-out time in seconds
	 */
	void fadeOut(int fadeOut);
	
	/**
	 * Panning feature: spreading of the SID chip sound signal to the two stereo
	 * channels
	 * 
	 * @param sidNum
	 *            SID chip number
	 */
	void setBalance(int sidNum);

	/**
	 * Doubles speed factor.
	 */
	void fastForward();

	/**
	 * Use normal speed factor.
	 */
	void normalSpeed();

	/**
	 * @return speed factor is used?
	 */
	boolean isFastForward();

}
