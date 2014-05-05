package sidplay.audio;

public enum Output {
	/** No audio. */
	OUT_NULL(new AudioNull()),
	/** Java Sound API. */
	OUT_SOUNDCARD(new JavaSound()),
	/** WAV file write. */
	OUT_WAV(new WavFile()),
	/** MP3 file write. */
	OUT_MP3(new MP3File()),
	/** Java Sound API plus WAV file write. */
	OUT_LIVE_WAV(new ProxyDriver(new JavaSound(), new WavFile())),
	/** Java Sound API and MP3 file write. */
	OUT_LIVE_MP3(new ProxyDriver(new JavaSound(), new MP3File())),
	/** Java Sound API plus recording playback. */
	OUT_COMPARE(new CmpMP3File());

	private final AudioDriver drv;

	private Output(AudioDriver drv) {
		this.drv = drv;
	}

	public AudioDriver getDriver() {
		return drv;
	}
}