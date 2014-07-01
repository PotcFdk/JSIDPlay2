package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TitledPane;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import resid_builder.ReSID;
import resid_builder.resid.SID;
import resid_builder.resid.WaveformGenerator;
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
		if (sidemu instanceof ReSID) {
			SID sid = ((ReSID) sidemu).sid();
			final WaveformGenerator wave = sid.voice[getVoice()].wave;
			int sampleValue = wave.readOSC() & 0xff;
			accumulate(sampleValue / 255f);
		} else if (sidemu instanceof residfp_builder.ReSID) {
			residfp_builder.resid.SID sid = ((residfp_builder.ReSID) sidemu)
					.sid();
			final residfp_builder.resid.WaveformGenerator wave = sid.voice[getVoice()].wave;
			int sampleValue = wave.readOSC(sid.getChipModel()) & 0xff;
			accumulate(sampleValue / 255f);
		} else {
			accumulate((byte) 0);
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