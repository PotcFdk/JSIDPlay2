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

import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

/**
 * Base class for hardware or software based SID emulation. All register write
 * access is recorded and can be read by {@link #readInternalRegister(int)}
 * (side-effect free).
 * 
 * @author ken
 *
 */
public abstract class SIDEmu {

	/** no SID chip */
	public static final SIDEmu NONE = null;
	
	/** SID base address */
	public static final int DEF_BASE_ADDRESS = 0xd400;

	/** Number of SID chip registers. */
	public final static int REG_COUNT = 32;

	/** Event context */
	protected final EventScheduler context;

	/** Last time chip was accessed. */
	protected long lastTime;

	/** Internal cache of SID register state, used for GUI feedback. */
	private final byte[] registers = new byte[REG_COUNT];

	/** SID chip number (0..MAX_SIDS-1) */
	private int sidNum;
	/** SID chip base address */
	private int baseAddress = DEF_BASE_ADDRESS;
	/** SID chip listener */
	private SIDListener listener;
	
	public SIDEmu(EventScheduler context) {
		this.context = context;
	}

	public int getBaseAddress() {
		return baseAddress;
	}
	
	public void setBaseAddress(int baseAddress) {
		this.baseAddress = baseAddress;
	}

	public void setListener(int sidNum, SIDListener listener) {
		this.listener = listener;
		this.sidNum = sidNum;
	}
	
	/**
	 * Side effect free read access.
	 * 
	 * @param addr
	 *            address to read
	 * @return register value recorded since last write access
	 */
	public byte readInternalRegister(final int addr) {
		return registers[addr];
	}

	public void write(final int addr, final byte data) {
		registers[addr] = data;
		if (listener != null) {
			final long time = context.getTime(Event.Phase.PHI2);
			listener.write(time, sidNum, addr, data);
		}
	}

	protected int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

	public void clearClocksSinceLastAccess() {
		lastTime = context.getTime(Event.Phase.PHI2);
	}

	public abstract void reset(byte volume);

	public abstract byte read(int addr);

	public abstract void clock();

	public abstract void setChipModel(final ChipModel model);

	public abstract void setClockFrequency(double cpuFrequency);

	public abstract void input(int input);

	public abstract int getInputDigiBoost();

	public abstract void setVoiceMute(int num, boolean mute);

	public abstract void setFilter(IConfig config, int sidNum);

	public abstract void setFilterEnable(IEmulationSection emulation, int sidNum);

}
