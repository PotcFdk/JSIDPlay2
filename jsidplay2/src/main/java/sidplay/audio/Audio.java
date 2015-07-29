package sidplay.audio;

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

	private final AudioDriver audioDriver;

	private Audio(AudioDriver audioDriver) {
		this.audioDriver = audioDriver;
	}

	public final AudioDriver getAudioDriver() {
		return audioDriver;
	}
}