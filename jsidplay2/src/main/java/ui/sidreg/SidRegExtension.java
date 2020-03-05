package ui.sidreg;
import static sidplay.audio.SidRegDriver.BUNDLE;
import static sidplay.audio.SidRegDriver.DESCRIPTION;

import libsidplay.common.SIDListener;

public abstract class SidRegExtension implements SIDListener {

	private long fTime;

	@Override
	public void write(final long time, final int addr, final byte data) {

		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;
		final SidRegWrite row = new SidRegWrite(time, relTime, addr, BUNDLE.getString(DESCRIPTION[addr & 0x1f]),
				data & 0xff);

		sidWrite(row);

		fTime = time;
	}

	public void init() {
		clear();
		fTime = 0;
	}

	public abstract void clear();

	public abstract void sidWrite(final SidRegWrite output);

}
