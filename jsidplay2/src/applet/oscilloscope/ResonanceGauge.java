package applet.oscilloscope;

import libsidplay.common.SIDEmu;

public final class ResonanceGauge extends SIDGauge {
	public ResonanceGauge() {
		super();
	}

	@Override
	public void sample(SIDEmu sidemu) {
		int res = (sidemu.readInternalRegister(0x17) >> 4) & 0xf;
		accumulate(res / 15f);
	}
}