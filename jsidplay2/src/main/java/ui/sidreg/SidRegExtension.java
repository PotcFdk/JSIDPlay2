package ui.sidreg;

import libsidplay.common.SIDListener;
import sidplay.audio.SidRegDriver.SidRegWrite;

public abstract class SidRegExtension implements SIDListener {

	private long fTime;

	@Override
	public void write(final long time, final int addr, final byte data) {

		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;

		sidWrite(new SidRegWrite(time, relTime, addr, data));

		fTime = time;
	}

	public void init() {
		clear();
		fTime = 0;
	}

	public abstract void clear();

	public abstract void sidWrite(final SidRegWrite output);

}
