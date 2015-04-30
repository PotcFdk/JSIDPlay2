package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import ui.common.C64Window;

public final class VolumeGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	public VolumeGauge(C64Window window, Player player) {
		super(window, player);
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
	public SIDGauge sample(SIDEmu sidemu) {
		int sample = sidemu.readInternalRegister(0x18) & 0xf;
		accumulate(sample / 15f);
		return this;
	}
}