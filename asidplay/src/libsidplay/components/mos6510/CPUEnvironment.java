/**
 *                        This is the environment file which
 *                        defines all the standard functions
 *                           to be inherited by the ICs.
 *                        ----------------------------------
 *  begin                : Thu May 11 2000
 *  copyright            : (C) 2000 by Simon White
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
package libsidplay.components.mos6510;

public interface CPUEnvironment {
	/* env funcs */
	byte cpuReadMemory(int addr);

	void cpuWriteMemory(int addr, byte data);
}
