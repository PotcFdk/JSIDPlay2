package libsidplay.player;

import java.io.File;

import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import sidplay.audio.Audio;
import sidplay.audio.CmpMP3File;
import sidplay.ini.intf.IConfig;

public class DriverSettings {
	/** SID emulation */
	protected Emulation emulation;
	/** output */
	protected Audio audio;

	private DriverSettings oldDriverSettings;

	public DriverSettings(Audio audio, Emulation emulation) {
		this.audio = audio;
		this.emulation = emulation;
	}

	public final Emulation getEmulation() {
		return emulation;
	}

	public final void setEmulation(final Emulation emulation) {
		this.emulation = emulation;
	}

	public final Audio getAudio() {
		return audio;
	}

	public final void setAudio(final Audio audio) {
		this.audio = audio;
	}

	public final void handleMP3(IConfig config, SidTune tune) {
		if (oldDriverSettings != null) {
			// restore settings after MP3 has been played last time
			restore(oldDriverSettings);
			oldDriverSettings = null;
		}
		if (tune != null && tune instanceof MP3Tune) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			oldDriverSettings = save();

			audio = Audio.COMPARE;
			emulation = Emulation.RESID;
			config.getAudio().setPlayOriginal(true);
			config.getAudio().setMp3File(((MP3Tune) tune).getMP3Filename());
		}
		if (audio.getAudioDriver() instanceof CmpMP3File) {
			// Set MP3 comparison settings
			CmpMP3File cmpMp3Driver = (CmpMP3File) audio.getAudioDriver();
			cmpMp3Driver.setPlayOriginal(config.getAudio().isPlayOriginal());
			cmpMp3Driver.setMp3File(new File(config.getAudio().getMp3File()));
		}
	}

	private DriverSettings save() {
		return new DriverSettings(this.audio, this.emulation);
	}

	private void restore(DriverSettings oldDriverSettings) {
		this.audio = oldDriverSettings.audio;
		this.emulation = oldDriverSettings.emulation;
	}

}