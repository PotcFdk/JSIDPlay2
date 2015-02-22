package sidplay.audio;

public enum Audio {
	/** Java Sound API. */
	SOUNDCARD(new JavaSound(), "Soundcard"),
	/** WAV file write. */
	WAV(new WavFile(), "WAV"),
	/** MP3 file write. */
	MP3(new MP3File(), "MP3"),
	/** Java Sound API plus WAV file write. */
	LIVE_WAV(new ProxyDriver(new JavaSound(), new WavFile()), "Record WAV"),
	/** Java Sound API plus MP3 file write. */
	LIVE_MP3(new ProxyDriver(new JavaSound(), new MP3File()), "Record MP3"),
	/** Java Sound API plus playback of MP3 recording. */
	COMPARE_MP3(new CmpMP3File(), "Comparison");

	private final AudioDriver audioDriver;
	private final String description;

	private Audio(AudioDriver audioDriver, String description) {
		this.audioDriver = audioDriver;
		this.description = description;
	}

	public final AudioDriver getAudioDriver() {
		return audioDriver;
	}

	@Override
	public String toString() {
		return description;
	}
}