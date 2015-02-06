/**
 *             ReSID builder class for creating/controlling resids
 *             ---------------------------------------------------
 *  begin                : Wed Sep 5 2001
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
package resid_builder;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.ReSIDBuilderBase;
import libsidplay.common.SIDEmu;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IConfig;

public class ReSIDBuilder extends ReSIDBuilderBase {
	public ReSIDBuilder(IConfig config, AudioConfig audioConfig,
			CPUClock cpuClock, AudioDriver audio, SidTune tune) {
		super(config, audioConfig, cpuClock, audio, tune);
	}

	@Override
	protected SIDEmu createSIDEmu(EventScheduler context) {
		return new ReSID(context, this);
	}

}
