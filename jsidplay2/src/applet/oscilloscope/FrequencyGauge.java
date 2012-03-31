package applet.oscilloscope;

import libsidplay.common.SIDEmu;

public final class FrequencyGauge extends SIDGauge {
	public FrequencyGauge() {
		super();
	}

	@Override
	public void sample(SIDEmu sidemu) {
		int frqValue = (sidemu.readInternalRegister(1 + v * 7) & 0xff) << 8
				| sidemu.readInternalRegister(0 + v * 7) & 0xff;
		float frq = 12 * 7;
		if (frqValue != 0) {
			frq = (float) (Math.log(frqValue / 65535.0f) / Math.log(2) * 12);
		}
		accumulate(1f + frq / (12 * 7));
	}
}