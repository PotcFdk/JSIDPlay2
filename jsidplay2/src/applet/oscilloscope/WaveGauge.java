package applet.oscilloscope;

import libsidplay.common.SIDEmu;
import resid_builder.ReSID;
import resid_builder.resid.SID;
import resid_builder.resid.WaveformGenerator;

public final class WaveGauge extends SIDGauge {
	public WaveGauge() {
		super();
	}

	@Override
	public void sample(SIDEmu sidemu) {
		if (!(sidemu instanceof ReSID)) {
			accumulate((byte) 0);
			return;
		}

		SID sid = ((ReSID) sidemu).sid();
		final WaveformGenerator wave = sid.voice[v].wave;
		int sampleValue = wave.readOSC() & 0xff;
		accumulate(sampleValue / 255f);
	}

	@Override
	public void updateGauge(SIDEmu sidemu) {
		super.updateGauge();
		if (sidemu != null) {
			final byte wf = sidemu.readInternalRegister(4 + 7 * v);
			final byte filt = sidemu.readInternalRegister(0x17);
			setLabel(String.format(
					localizer.getString("WAVE") + " %X %s%s%s%s",
					wf >> 4 & 0xf, (wf & 2) != 0 ? "S" : "",
					(wf & 4) != 0 ? "R" : "", (wf & 8) != 0 ? "T" : "",
					(filt & 1 << v) != 0 ? "F" : ""));
		}
	}
}