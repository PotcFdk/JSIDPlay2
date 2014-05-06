package libsidplay.player;

import java.io.File;
import java.util.Locale;

import libsidplay.sidtune.SidTune;
import sidplay.audio.CmpMP3File;
import sidplay.audio.Output;
import sidplay.ini.intf.IConfig;

public class DriverSettings {
	/** SID emulation */
	protected Emulation emulation;
	/** output */
	protected Output output;

	private DriverSettings oldDriverSettings;

	public DriverSettings(Output output, Emulation emulation) {
		this.emulation = emulation;
		this.output = output;
	}

	public final Emulation getEmulation() {
		return emulation;
	}

	public final void setEmulation(final Emulation emulation) {
		this.emulation = emulation;
	}

	public final Output getOutput() {
		return output;
	}

	public final void setOutput(final Output output) {
		this.output = output;
	}

	public final void handleMP3(IConfig config, SidTune tune) {
		if (oldDriverSettings != null) {
			// restore settings after MP3 has been played last time
			restore(oldDriverSettings);
			oldDriverSettings = null;
		}
		if (tune != null
				&& tune.getInfo().file != null
				&& tune.getInfo().file.getName().toLowerCase(Locale.ENGLISH)
						.endsWith(".mp3")) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			oldDriverSettings = save();

			output = Output.OUT_COMPARE;
			emulation = Emulation.EMU_RESID;
			config.getAudio().setPlayOriginal(true);
			config.getAudio().setMp3File(tune.getInfo().file.getAbsolutePath());
		}
		if (output.getDriver() instanceof CmpMP3File) {
			// Set MP3 comparison settings
			CmpMP3File cmpMp3Driver = (CmpMP3File) output.getDriver();
			cmpMp3Driver.setPlayOriginal(config.getAudio().isPlayOriginal());
			cmpMp3Driver.setMp3File(new File(config.getAudio().getMp3File()));
		}
	}

	private DriverSettings save() {
		return new DriverSettings(this.output, this.emulation);
	}

	private void restore(DriverSettings oldDriverSettings) {
		this.output = oldDriverSettings.output;
		this.emulation = oldDriverSettings.emulation;
	}

}