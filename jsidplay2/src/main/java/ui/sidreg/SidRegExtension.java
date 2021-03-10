package ui.sidreg;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import sidplay.audio.SIDRegDriver.SidRegWrite;

public abstract class SidRegExtension implements SIDListener {

	private EventScheduler context;
	private long fTime;

	@Override
	public void write(final int addr, final byte data) {
		final long time = context.getTime(Event.Phase.PHI2);

		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;

		sidWrite(new SidRegWrite(time, relTime, addr, data));

		fTime = time;
	}

	public void init(EventScheduler context) {
		clear();
		this.context = context;
		this.fTime = 0;
	}

	public abstract void clear();

	public abstract void sidWrite(final SidRegWrite output);

}
