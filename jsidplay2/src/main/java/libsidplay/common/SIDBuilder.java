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
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IEmulationSection;

/**
 * @author Ken Händel
 * 
 *         Implement this class to create a new SID emulations for libsidplay2.
 */
public interface SIDBuilder {
	/**
	 * Create a new SID chip emulation.
	 * @param emulationSection 
	 * 
	 * @return emulated SID chip
	 */
	SIDEmu lock(EventScheduler context, IEmulationSection emulationSection,
			SIDEmu device, int sidNum, SidTune tune);

	/**
	 * Destroy SID chip emulation.
	 */
	void unlock(SIDEmu device);

	/**
	 * @return current number of devices.
	 */
	int getNumDevices();

	/**
	 * Timer start reached, audio output should be processed.
	 */
	void start(EventScheduler context);

	/**
	 * Volume of the SID chip
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            0(-6db)..12(+6db)
	 */
	void setVolume(int sidNum, IAudioSection audio);

	/**
	 * Panning feature: spreading of the SID chip sound signal to the two stereo
	 * channels
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            0(left speaker)..0.5(centered)..1(right speaker)
	 */
	void setBalance(int sidNum, IAudioSection audio);

}
