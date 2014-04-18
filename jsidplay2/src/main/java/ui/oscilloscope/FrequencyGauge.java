package ui.oscilloscope;

import sidplay.ConsolePlayer;
import ui.common.C64Stage;
import ui.entities.config.Configuration;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;

public final class FrequencyGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	public FrequencyGauge(C64Stage c64Stage, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		super(c64Stage, consolePlayer, player, config);
	}

	@Override
	protected Canvas getArea() {
		return area;
	}

	@Override
	protected TitledPane getTitledPane() {
		return border;
	}

	@Override
	public void sample(SIDEmu sidemu) {
		int frqValue = (sidemu.readInternalRegister(1 + getVoice() * 7) & 0xff) << 8
				| sidemu.readInternalRegister(0 + getVoice() * 7) & 0xff;
		float frq = 12 * 7;
		if (frqValue != 0) {
			frq = (float) (Math.log(frqValue / 65535.0f) / Math.log(2) * 12);
		}
		accumulate(1f + frq / (12 * 7));
	}
}