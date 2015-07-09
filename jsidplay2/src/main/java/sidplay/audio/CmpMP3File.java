package sidplay.audio;

import java.io.File;
import java.io.IOException;

import lowlevel.LameDecoder;

/**
 * Sound driver to listen to emulation and MP3 recording in parallel.
 * 
 * @author Ken Händel
 * 
 */
public class CmpMP3File extends JavaSound {

	/**
	 * Play MP3 (true) or emulation (false).
	 */
	private boolean playOriginal;
	/**
	 * MP3 file to play.
	 */
	private File mp3File;

	/**
	 * Jump3r decoder.
	 */
	protected LameDecoder jump3r;
	/**
	 * MP3 sound output.
	 */
	protected JavaSound mp3JavaSound = new JavaSound();

	@Override
	public void open(final AudioConfig cfg, String recordingFilename)
			throws IOException {
		super.open(cfg, recordingFilename);

		jump3r = new LameDecoder(mp3File.getAbsolutePath());

		mp3JavaSound.open(
				new AudioConfig(jump3r.getSampleRate(), jump3r.getChannels(),
						cfg.getDevice()) {
					@Override
					public int getChunkFrames() {
						return jump3r.getFrameSize();
					}

					@Override
					public int getFrameRate() {
						return jump3r.getSampleRate();
					}
				}, recordingFilename);
	}

	@Override
	public void write() throws InterruptedException {
		if (!jump3r.decode(mp3JavaSound.buffer())) {
			throw new InterruptedException();
		}
		if (playOriginal) {
			mp3JavaSound.write();
		} else {
			super.write();
		}
	}

	@Override
	public synchronized void pause() {
		super.pause();
		mp3JavaSound.pause();
	}

	@Override
	public void close() {
		super.close();
		mp3JavaSound.close();
		if (jump3r != null) {
			jump3r.close();
		}
	}

	public void setPlayOriginal(final boolean playOriginal) {
		this.playOriginal = playOriginal;
	}

	public void setMp3File(File mp3File) {
		this.mp3File = mp3File;
	}

}
