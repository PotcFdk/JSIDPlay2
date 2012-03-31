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

import static libsidplay.common.ISID2Types.sid2_model_t.SID2_MODEL_CORRECT;
import static libsidplay.common.ISID2Types.sid2_playback_t.sid2_mono;

public interface ISID2Types {
	/**
	 * Default output bit depth
	 */
	final int SID2_DEFAULT_PRECISION = 16;

	//
	// Default settings
	//

	final int SID2_DEFAULT_SAMPLING_FREQ = 48000;

	//
	// Types
	//
	enum sid2_playback_t {
		sid2_mono, sid2_stereo
	}

	enum sid2_model_t {
		SID2_MODEL_CORRECT, SID2_MOS6581, SID2_MOS8580
	}

	enum sid2_clock_t {
		SID2_CLOCK_PAL, SID2_CLOCK_NTSC
	}

	class sid2_config_t {
		/**
		 * Default emulation speed. Tune can override this unless forced.
		 */
		public sid2_clock_t clockSpeed = sid2_clock_t.SID2_CLOCK_PAL;

		/**
		 * Force use of clockSpeed.
		 */
		public boolean clockForced = false;

		public int frequency = SID2_DEFAULT_SAMPLING_FREQ;

		public sid2_playback_t playback = sid2_mono;

		/**
		 * Intended sid model when unknown
		 */
		public sid2_model_t sidDefault = SID2_MODEL_CORRECT;

		/**
		 * User requested sid model
		 */
		public sid2_model_t sidModel = SID2_MODEL_CORRECT;

		/**
		 * Default base address of dual SID
		 */
		public int dualSidBase;

		/**
		 * Force the tune to be a stereo tune (dual SID)
		 */
		public boolean forceStereoTune;
	}
}