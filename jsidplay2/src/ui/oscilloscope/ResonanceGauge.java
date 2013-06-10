package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.common.SIDEmu;

public final class ResonanceGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	@Override
	protected Canvas getArea() {
		return area;
	}

	@Override
	protected TitledPane getBorder() {
		return border;
	}

	@Override
	public void sample(SIDEmu sidemu) {
		int res = (sidemu.readInternalRegister(0x17) >> 4) & 0xf;
		accumulate(res / 15f);
	}
}