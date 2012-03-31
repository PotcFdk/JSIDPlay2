package applet.oscilloscope;

import libsidplay.common.SIDEmu;

public final class FilterGauge extends SIDGauge {
	public FilterGauge() {
		super();
	}

	@Override
	public void sample(SIDEmu sidemu) {
		int fc = sidemu.readInternalRegister(0x15) & 7;
		fc |= (sidemu.readInternalRegister(0x16) & 0xff) << 3;
		accumulate(fc / 2047.0f);
	}

	@Override
	public void updateGauge(SIDEmu sidemu) {
		super.updateGauge();
		if (sidemu != null) {
			final byte vol = sidemu.readInternalRegister(0x18);
			setLabel((localizer != null ? localizer.getString("FILTER") : "")
					+ " " + ((vol & 0x10) != 0 ? "L" : "")
					+ ((vol & 0x20) != 0 ? "B" : "")
					+ ((vol & 0x40) != 0 ? "H" : ""));
		}
	}
}