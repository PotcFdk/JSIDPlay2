package sidplay.consoleplayer;

import sidplay.audio.AudioDriver;
import sidplay.audio.AudioNull;
import sidplay.audio.CmpMP3File;
import sidplay.audio.JavaSound;
import sidplay.audio.MP3File;
import sidplay.audio.ProxyDriver;
import sidplay.audio.WavFile;

public enum Output {
	/** No audio. */
	OUT_NULL(false, new AudioNull()),
	/** Java Sound API. */
	OUT_SOUNDCARD(false, new JavaSound()),
	/** WAV file write. */
	OUT_WAV(true, new WavFile()),
	/** MP3 file write. */
	OUT_MP3(true, new MP3File()),
	/** Java Sound API plus WAV file write. */
	OUT_LIVE_WAV(true, new ProxyDriver(new JavaSound(), new WavFile())),
	/** Java Sound API and MP3 file write. */
	OUT_LIVE_MP3(true, new ProxyDriver(new JavaSound(), new MP3File())),
	/** Java Sound API plus recording playback. */
	OUT_COMPARE(false, new CmpMP3File());

	private final boolean fileBased;
	private final AudioDriver drv;

	private Output(boolean fileBased, AudioDriver drv) {
		this.fileBased = fileBased;
		this.drv = drv;
	}

	public boolean isFileBased() {
		return fileBased;
	}

	public AudioDriver getDriver() {
		return drv;
	}
}