package ui.oscilloscope;

import java.util.ResourceBundle;

import sidplay.ConsolePlayer;
import ui.common.C64Stage;
import ui.entities.config.Configuration;
import libsidplay.Player;
import libsidplay.common.SIDEmu;

abstract class SIDGauge extends Gauge {
	protected ResourceBundle localizer;

	public SIDGauge(C64Stage c64Stage, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		super(c64Stage, consolePlayer, player, config);
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
	public abstract void sample(SIDEmu sid);

	/**
	 * Redraw gauge. SID is passed to maybe update label etc.
	 * 
	 * @param sid
	 *            SID to redraw the gauge for.
	 */
	public void updateGauge(SIDEmu sid) {
		updateGauge();
	}

	public void setLocalizer(ResourceBundle resources) {
		localizer = resources;
	}
}