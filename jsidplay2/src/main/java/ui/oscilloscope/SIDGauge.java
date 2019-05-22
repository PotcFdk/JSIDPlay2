package ui.oscilloscope;

import java.util.ResourceBundle;

import javafx.scene.image.Image;
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

	@Override
	public void sample() {
	}

	/**
	 * Sample audio from provided SID.
	 * 
	 * @param sid
	 *            The SID to sample audio from.
	 */
	public abstract SIDGauge sample(SIDEmu sid);

	/**
	 * Redraw gauge. SID is passed to maybe update label etc.
	 * 
	 * @param sid
	 *            SID to redraw the gauge for.
	 */
	public void updateGauge(SIDEmu sid, Image image) {
		super.updateGauge(sid, image);
	}

	public void setLocalizer(ResourceBundle resources) {
		localizer = resources;
	}
}