package sidplay.audio;

import java.io.IOException;
import java.nio.Buffer;
import java.util.Arrays;

import javax.sound.sampled.LineUnavailableException;

import builder.resid.SIDMixer;
import libsidplay.common.CPUClock;
import libsidplay.config.IAudioSection;
import lowlevel.LameDecoder;

/**
 * Sound driver to listen to emulation and MP3 recording in parallel.
 * 
 * @author Ken Händel
 * 
 */
public class CmpMP3File extends JavaSound {

	public static class MP3Termination extends InterruptedException {
		private static final long serialVersionUID = -7204524330347735933L;

		public MP3Termination() {
		}

		public MP3Termination(Exception e) {
			super(e.getMessage());
		}
	}

	/**
	 * Jump3r decoder.
	 */
	protected LameDecoder jump3r;
	/**
	 * MP3 sound output.
	 */
	protected JavaSound mp3JavaSound = new JavaSound();

	private IAudioSection audioSection;

	void setAudioSection(final IAudioSection audioSection) {
		this.audioSection = audioSection;
	}

	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		super.open(cfg.setBufferFrames((1 << SIDMixer.MAX_FAST_FORWARD) * cfg.getChannels() * 64)
				.setChunkFrames(2048), recordingFilename, cpuClock);

		jump3r = new LameDecoder(audioSection.getMp3File());

		mp3JavaSound.open(new AudioConfig(jump3r.getSampleRate(), jump3r.getChannels(), cfg.getDevice()) {
			@Override
			public int getChunkFrames() {
				return jump3r.getFrameSize();
			}

		}, recordingFilename, cpuClock);
	}

	@Override
	public void write() throws InterruptedException {
		// repair sound after switching between original and emu
		for (JavaSound javaSound : Arrays.asList(this, mp3JavaSound)) {
			if (javaSound.dataLine.available() == 0) {
				javaSound.dataLine.close();
				try {
					javaSound.dataLine.open(javaSound.dataLine.getFormat(),
							javaSound.cfg.getBufferFrames() * Short.BYTES * javaSound.cfg.getChannels());
				} catch (LineUnavailableException e) {
					throw new RuntimeException(e.getMessage());
				}
			}
		}
		if (!jump3r.decode(mp3JavaSound.buffer())) {
			throw new MP3Termination();
		}
		((Buffer) mp3JavaSound.buffer()).position(mp3JavaSound.buffer().limit());
		if (audioSection.isPlayOriginal()) {
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

}
