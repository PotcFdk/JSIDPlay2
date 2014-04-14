package ui.oscilloscope;

import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;

public final class FilterGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	public FilterGauge(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		super(consolePlayer, player, config);
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
		int fc = sidemu.readInternalRegister(0x15) & 7;
		fc |= (sidemu.readInternalRegister(0x16) & 0xff) << 3;
		accumulate(fc / 2047.0f);
	}

	@Override
	public void updateGauge(SIDEmu sidemu) {
		super.updateGauge();
		if (sidemu != null) {
			final byte vol = sidemu.readInternalRegister(0x18);
			setText((localizer != null ? localizer.getString("FILTER") : "")
					+ " " + ((vol & 0x10) != 0 ? "L" : "")
					+ ((vol & 0x20) != 0 ? "B" : "")
					+ ((vol & 0x40) != 0 ? "H" : ""));
		}
	}
}