package applet.oscilloscope;

import org.swixml.Localizer;

import libsidplay.common.SIDEmu;

abstract class SIDGauge extends Gauge {
	protected int v;
	protected Localizer localizer;

	public SIDGauge() {
		super();
	}

	public void setVoice(int voice) {
		v = voice;
	}

	@Override
	public void sample() {
	}

	/**
	 * Sample audio from provided SID.
	 * 
	 * @param sid
	 */
	public abstract void sample(SIDEmu sid);

	/**
	 * Redraw gauge. SID is passed to maybe update label etc.
	 * 
	 * @param sid
	 */
	public void updateGauge(SIDEmu sid) {
		updateGauge();
	}

	public void setLocalizer(Localizer l) {
		localizer = l;
	}
}