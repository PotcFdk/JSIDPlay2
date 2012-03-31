package applet.oscilloscope;

import libsidplay.common.SIDEmu;

public final class VolumeGauge extends SIDGauge {
	public VolumeGauge() {
		super();
	}

	@Override
	public void sample(SIDEmu sidemu) {
		int sample = sidemu.readInternalRegister(0x18) & 0xf;
		accumulate(sample / 15f);
	}
}