package applet.oscilloscope;

import libsidplay.common.SIDEmu;
import resid_builder.ReSID;
import resid_builder.resid.SID;

public final class EnvelopeGauge extends SIDGauge {
	public EnvelopeGauge() {
		super();
	}

	@Override
	public void sample(SIDEmu sidemu) {
		if (!(sidemu instanceof ReSID)) {
			accumulate((byte) 0);
			return;
		}

		SID sid = ((ReSID) sidemu).sid();
		final byte envOutput = sid.voice[v].envelope.readENV();
		float value = -48;
		if (envOutput != 0) {
			value = (float) (Math.log((envOutput & 0xff) / 255f) / Math.log(10) * 20);
		}
		accumulate(1f + value / 48.0f);
	}
}