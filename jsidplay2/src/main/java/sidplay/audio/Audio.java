package sidplay.audio;

import java.io.File;

import libsidplay.config.IAudioSection;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import sidplay.audio.MP3Driver.MP3File;

public enum Audio {
	/** Java Sound API. */
	SOUNDCARD(new JavaSound()),
	/** WAV file write. */
	WAV(new WavFile()),
	/** MP3 file write. */
	MP3(new MP3File()),
	/** Java Sound API plus WAV file write. */
	LIVE_WAV(new ProxyDriver(new JavaSound(), new WavFile())),
	/** Java Sound API plus MP3 file write. */
	LIVE_MP3(new ProxyDriver(new JavaSound(), new MP3File())),
	/** Java Sound API plus playback of MP3 recording. */
	COMPARE_MP3(new CmpMP3File());

	private AudioDriver oldAudioDriver;

	private Audio(AudioDriver audioDriver) {
		this.audioDriver = audioDriver;
	}

	public final AudioDriver getAudioDriver() {
		return audioDriver;
	}

	private final AudioDriver audioDriver;

	public final AudioDriver getAudioDriver(final IAudioSection audioSection, SidTune tune) {
		return handleMP3(audioSection, tune, audioDriver);
	}

	/**
	 * MP3 play-back is using the COMPARE audio driver. Old settings are saved
	 * (playing mp3) and restored (next time normal tune is played).
	 */
	private AudioDriver handleMP3(final IAudioSection audioSection, final SidTune tune, final AudioDriver audioDriver) {
		AudioDriver newAudioDriver = audioDriver;
		if (oldAudioDriver == null && tune instanceof MP3Tune) {
			// save settings before MP3 gets played
			oldAudioDriver = audioDriver;
		} else if (oldAudioDriver != null && !(tune instanceof MP3Tune)) {
			// restore settings after MP3 has been played last time
			newAudioDriver = oldAudioDriver;
			oldAudioDriver = null;
		}
		if (tune instanceof MP3Tune) {
			// Change driver settings to use comparison driver for MP3 play-back
			MP3Tune mp3Tune = (MP3Tune) tune;
			newAudioDriver = COMPARE_MP3.getAudioDriver();
			audioSection.setPlayOriginal(true);
			audioSection.setMp3File(mp3Tune.getMP3Filename());
		}
		if (newAudioDriver instanceof CmpMP3File) {
			// Configure compare driver settings
			CmpMP3File cmp = (CmpMP3File) newAudioDriver;
			cmp.setPlayOriginal(audioSection.isPlayOriginal());
			cmp.setMp3File(new File(audioSection.getMp3File()));
		}
		return newAudioDriver;
	}

}