package sidplay.audio;

import java.io.File;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.sidtune.SidTune;
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
	protected JavaSound mp3Driver = new JavaSound();

	@Override
	public void open(final AudioConfig cfg, SidTune tune)
			throws LineUnavailableException {
		super.open(cfg, tune);

		jump3r = new LameDecoder(mp3File.getAbsolutePath());

		mp3Driver.open(
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
				}, tune);
	}

	@Override
	public void write() throws InterruptedException {
		if (!jump3r.decode(mp3Driver.buffer())) {
			throw new NaturalFinishedException();
		}
		if (playOriginal) {
			mp3Driver.write();
		} else {
			super.write();
		}
	}

	@Override
	public synchronized void pause() {
		super.pause();
		mp3Driver.pause();
	}

	@Override
	public void close() {
		super.close();
		mp3Driver.close();
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
