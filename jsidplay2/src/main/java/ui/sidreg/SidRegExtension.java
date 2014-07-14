package ui.sidreg;

import java.util.ResourceBundle;

import libsidplay.common.ReSIDExtension;

public abstract class SidRegExtension implements ReSIDExtension {

	private ResourceBundle bundle;

	private final String description[] = new String[] { "VOICE_1_FREQ_L",
			"VOICE_1_FREQ_H", "VOICE_1_PULSE_L", "VOICE_1_PULSE_H",
			"VOICE_1_CTRL", "VOICE_1_AD", "VOICE_1_SR", "VOICE_2_FREQ_L",
			"VOICE_2_FREQ_H", "VOICE_2_PULSE_L", "VOICE_2_PULSE_H",
			"VOICE_2_CTRL", "VOICE_2_AD", "VOICE_2_SR", "VOICE_3_FREQ_L",
			"VOICE_3_FREQ_H", "VOICE_3_PULSE_L", "VOICE_3_PULSE_H",
			"VOICE_3_CTRL", "VOICE_3_AD", "VOICE_3_SR", "FCUT_L", "FCUT_H",
			"FRES", "FVOL", "PADDLE1", "PADDLE2", "OSC3", "ENV3", };

	private long fTime;

	public void setbundle(ResourceBundle l) {
		bundle = l;
	}

	@Override
	public void write(final long time, final int chipNum, final int addr,
			final byte data) {
		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;
		final SidRegWrite row = new SidRegWrite(time, relTime,
				chipNum, bundle.getString(description[addr]), String.format(
						"$%02X", data & 0xff));

		sidWrite(row);

		fTime = time;
	}

	public void init() {
		clear();
		fTime = 0;
	}

	public abstract void clear();

	public abstract void sidWrite(final SidRegWrite output);

}
