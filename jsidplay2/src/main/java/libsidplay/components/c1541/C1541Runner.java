package libsidplay.components.c1541;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

public abstract class C1541Runner extends Event {
	protected final EventScheduler c64Context, c1541Context;
	private int conversionFactor, accum;
	private long c64LastTime;

	public C1541Runner(final EventScheduler c64Context,
			final EventScheduler c1541Context) {
		super("C64 permits C1541 to continue");
		this.c64Context = c64Context;
		this.c1541Context = c1541Context;
		this.c64LastTime = c64Context.getTime(Phase.PHI2);
	}

	/**
	 * Return the number of clock ticks that 1541 should advance.
	 * 
	 * @param offset
	 *            adjust C64 cycles
	 * 
	 * @return The number of clock ticks that 1541 should advance.
	 */
	protected int updateSlaveTicks(long offset) {
		final long oldC64Last = c64LastTime;
		c64LastTime = c64Context.getTime(Phase.PHI2) + offset;

		accum += conversionFactor * (int) (c64LastTime - oldC64Last);
		int wholeClocks = accum >> 16;
		accum &= 0xffff;

		return wholeClocks;
	}

	public void setClockDivider(final CPUClock clock) {
		conversionFactor = (int) (1000000.0 / clock.getCpuFrequency() * 65536.0 + 0.5);
	}

	public void reset() {
		c64LastTime = c64Context.getTime(Phase.PHI2);
	}

	public abstract void cancel();

	public abstract void synchronize(long offset);
}