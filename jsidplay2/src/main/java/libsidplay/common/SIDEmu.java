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

import sidplay.ini.intf.IConfig;

public abstract class SIDEmu {
	/** Event context */
	protected final EventScheduler context;

	/** Last time chip was accessed. */
	protected long lastTime;

	/** Internal cache of SID register state, used for GUI feedback. */
	private final byte[] registers = new byte[32];

	public SIDEmu(EventScheduler context) {
		this.context = context;
	}

	public byte readInternalRegister(final int addr) {
		return registers[addr];
	}

	public void write(final int addr, final byte data) {
		registers[addr] = data;
	}

	protected int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

	public abstract void reset(byte volume);

	public abstract byte read(int addr);

	public abstract void clock();

	public abstract void setVoiceMute(int num, boolean mute);

	public abstract void setFilter(IConfig config);

	public abstract void setFilterEnable(boolean enable);

	public abstract ChipModel getChipModel();

	public abstract void setChipModel(final ChipModel model);

	public abstract void setSampling(double cpuFrequency, float frequency,
			SamplingMethod sampling);

	public abstract void input(int input);
}
