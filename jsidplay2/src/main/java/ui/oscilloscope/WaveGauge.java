package ui.oscilloscope;

import builder.resid.ReSIDBase;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.common.SIDEmu;
import sidplay.Player;
import ui.common.C64Window;

public final class WaveGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private Canvas area;

	public WaveGauge() {
	}

	public WaveGauge(C64Window window, Player player) {
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
			accumulate((((ReSIDBase) sidemu).readOSC(getVoice()) & 0xff) / 255f);
		} else {
			accumulate(0f);
		}
		return this;
	}

	@Override
	public void addImage(SIDEmu sidemu) {
		if (sidemu != null) {
			final byte wf = sidemu.readInternalRegister(4 + 7 * getVoice());
			final byte filt = sidemu.readInternalRegister(0x17);
			setText(createText(wf, filt));
		}
		super.addImage(sidemu);
	}

	private String createText(final byte wf, final byte filt) {
		StringBuilder result = new StringBuilder();
		result.append(localizer.getString("WAVE"));
		result.append(" ");
		result.append(Integer.toHexString(wf >> 4 & 0xf)); // WF
		result.append(" ");
		if ((wf & 2) != 0) {
			result.append("S"); // SYNC
		}
		if ((wf & 4) != 0) {
			result.append("R"); // RING MOD
		}
		if ((wf & 8) != 0) {
			result.append("T"); // TEST
		}
		if ((filt & 1 << getVoice()) != 0) {
			result.append("F"); // FILTER
		}
		return result.toString();
	}

}