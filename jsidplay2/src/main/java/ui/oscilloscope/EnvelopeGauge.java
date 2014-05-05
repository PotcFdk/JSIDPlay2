package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import resid_builder.ReSID;
import resid_builder.resid.SID;
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
	public void sample(SIDEmu sidemu) {
		if (!(sidemu instanceof ReSID)) {
			accumulate((byte) 0);
			return;
		}

		SID sid = ((ReSID) sidemu).sid();
		final byte envOutput = sid.voice[getVoice()].envelope.readENV();
		float value = -48;
		if (envOutput != 0) {
			value = (float) (Math.log((envOutput & 0xff) / 255f) / Math.log(10) * 20);
		}
		accumulate(1f + value / 48.0f);
	}
}