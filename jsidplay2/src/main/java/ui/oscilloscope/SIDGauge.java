package ui.oscilloscope;

import java.util.ResourceBundle;

import libsidplay.common.SIDEmu;
import sidplay.Player;
import ui.common.C64Window;

abstract class SIDGauge extends Gauge {
	protected ResourceBundle localizer;

	public SIDGauge() {
	}

	public SIDGauge(C64Window window, Player player) {
		super(window, player);
	}

	/**
	 * Sample audio from provided SID.
	 *
	 * @param sid The SID to sample audio from.
	 */
	public abstract SIDGauge sample(SIDEmu sid);

	public void setLocalizer(ResourceBundle resources) {
		localizer = resources;
	}
}