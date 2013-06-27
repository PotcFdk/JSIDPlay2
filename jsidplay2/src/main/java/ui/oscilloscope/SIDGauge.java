package ui.oscilloscope;

import java.util.ResourceBundle;

import libsidplay.common.SIDEmu;

abstract class SIDGauge extends Gauge {
	protected ResourceBundle localizer;

	@Override
	public void sample() {
	}

	/**
	 * Sample audio from provided SID.
	 * 
	 * @param sid The SID to sample audio from.
	 */
	public abstract void sample(SIDEmu sid);

	/**
	 * Redraw gauge. SID is passed to maybe update label etc.
	 * 
	 * @param sid SID to redraw the gauge for.
	 */
	public void updateGauge(SIDEmu sid) {
		updateGauge();
	}

	public void setLocalizer(ResourceBundle resources) {
		localizer = resources;
	}
}