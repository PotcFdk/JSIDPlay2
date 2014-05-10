package sidplay.audio;

public enum Audio {
	/** No audio. */
	NONE(new AudioNull()),
	/** Java Sound API. */
	SOUNDCARD(new JavaSound()),
	/** WAV file write. */
	WAV(new WavFile()),
	/** MP3 file write. */
	MP3(new MP3File()),
	/** Java Sound API plus WAV file write. */
	LIVE_WAV(new ProxyDriver(new JavaSound(), new WavFile())),
	/** Java Sound API and MP3 file write. */
	LIVE_MP3(new ProxyDriver(new JavaSound(), new MP3File())),
	/** Java Sound API plus recording playback. */
	COMPARE(new CmpMP3File());

	private final AudioDriver audioDriver;

	private Audio(AudioDriver audioDriver) {
		this.audioDriver = audioDriver;
	}

	public final AudioDriver getAudioDriver() {
		return audioDriver;
	}

}