package ui.oscilloscope;

import sidplay.ConsolePlayer;
import ui.common.C64Stage;
import ui.entities.config.Configuration;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;

public final class ResonanceGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	public ResonanceGauge(C64Stage c64Stage, ConsolePlayer consolePlayer,
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
		int res = (sidemu.readInternalRegister(0x17) >> 4) & 0xf;
		accumulate(res / 15f);
	}
}