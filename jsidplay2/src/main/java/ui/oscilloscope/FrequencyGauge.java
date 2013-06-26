package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.common.SIDEmu;

public final class FrequencyGauge extends SIDGauge {

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
		int frqValue = (sidemu.readInternalRegister(1 + getVoice() * 7) & 0xff) << 8
				| sidemu.readInternalRegister(0 + getVoice() * 7) & 0xff;
		float frq = 12 * 7;
		if (frqValue != 0) {
			frq = (float) (Math.log(frqValue / 65535.0f) / Math.log(2) * 12);
		}
		accumulate(1f + frq / (12 * 7));
	}
}