/**
 *                            sidplay2 specific types
 *                            -----------------------
 *  begin                : Fri Aug 10 2001
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


public interface ISID2Types {
	enum Clock {
		PAL(985248.4, 50), NTSC(1022727.14, 60);
		
		private final double frequency;
		private final double refresh;
		
		Clock(double frequency, double refresh) {
			this.frequency = frequency;
			this.refresh = refresh;
		}
		
		public double getCpuFrequency() {
			return frequency;
		}
		
		public double getCyclesPerFrame() {
			return frequency / refresh;
		}

		public double getRefresh() {
			return refresh;
		}
	}
}