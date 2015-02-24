package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import resid_builder.ReSIDBase;
import ui.common.C64Window;

public final class WaveGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	@Override
	protected Canvas getArea() {
		return area;
	}

	public WaveGauge(C64Window window, Player player) {
		super(window, player);
	}

	@Override
	protected TitledPane getTitledPane() {
		return border;
	}

	@Override
	public void sample(SIDEmu sidemu) {
		if (sidemu instanceof ReSIDBase) {
			accumulate((((ReSIDBase) sidemu).getSID().readOSC(getVoice()) & 0xff) / 255f);
		} else {
			accumulate(0f);
		}
	}

	@Override
	public void updateGauge(SIDEmu sidemu) {
		super.updateGauge();
		if (sidemu != null) {
			final byte wf = sidemu.readInternalRegister(4 + 7 * getVoice());
			final byte filt = sidemu.readInternalRegister(0x17);
			setText(String.format(localizer.getString("WAVE") + " %X %s%s%s%s",
					wf >> 4 & 0xf, (wf & 2) != 0 ? "S" : "",
					(wf & 4) != 0 ? "R" : "", (wf & 8) != 0 ? "T" : "",
					(filt & 1 << getVoice()) != 0 ? "F" : ""));
		}
	}

}