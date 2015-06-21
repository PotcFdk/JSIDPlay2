package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.common.SIDEmu;
import resid_builder.ReSIDBase;
import sidplay.Player;
import ui.common.C64Window;

public final class EnvelopeGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	public EnvelopeGauge(C64Window window, Player player) {
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
		if (sidemu instanceof ReSIDBase) {
			accumulate(getValue(((ReSIDBase)sidemu).getSID().readENV(getVoice())));
		} else {
			accumulate(0f);
		}
		return this;
	}

	private float getValue(final byte envOutput) {
		float value = -48;
		if (envOutput != 0) {
			value = (float) (Math.log((envOutput & 0xff) / 255f) / Math.log(10) * 20);
		}
		return 1f + value / 48.0f;
	}
}