package sidplay.audio;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

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
	private int factor;
	private ByteBuffer mp3Buffer;
	private ByteBuffer mp3BigBuffer;

	private IAudioSection audioSection;

	void setAudioSection(final IAudioSection audioSection) {
		this.audioSection = audioSection;
	}

	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		super.open(cfg, recordingFilename, cpuClock);

		jump3r = new LameDecoder(audioSection.getMp3File());
		mp3Buffer = ByteBuffer.allocate(jump3r.getFrameSize() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
		factor = cfg.getBufferFrames() / jump3r.getFrameSize();
		mp3BigBuffer = ByteBuffer.allocate(factor * mp3Buffer.capacity()).order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		((Buffer) mp3BigBuffer).clear();
		for (int i = 0; i < factor; i++) {
			if (!jump3r.decode(mp3Buffer)) {
				throw new MP3Termination();
			}
			((Buffer) mp3Buffer).clear();
			mp3BigBuffer.put(mp3Buffer);
		}
		if (audioSection.isPlayOriginal()) {
			((Buffer) buffer()).clear();
			((Buffer) mp3BigBuffer).flip();
			buffer().put(mp3BigBuffer);
			((Buffer) buffer()).position(mp3BigBuffer.limit());
		}
		super.write();
	}

	@Override
	public void close() {
		super.close();
		if (jump3r != null) {
			jump3r.close();
		}
	}

}
