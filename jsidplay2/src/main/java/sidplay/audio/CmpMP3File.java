package sidplay.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

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
		private static final long serialVersionUID = 1L;

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
	private ByteBuffer decodedMP3Buffer;
	private ByteBuffer mp3Buffer;

	private IAudioSection audioSection;

	void setAudioSection(final IAudioSection audioSection) {
		this.audioSection = audioSection;
	}

	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		super.open(cfg, recordingFilename, cpuClock);

		if (!new File(audioSection.getMp3File()).exists()) {
			throw new FileNotFoundException(audioSection.getMp3File());
		}
		jump3r = new LameDecoder(audioSection.getMp3File());
		decodedMP3Buffer = ByteBuffer.wrap(new byte[jump3r.getFrameSize() * Short.BYTES * jump3r.getChannels()])
				.order(ByteOrder.nativeOrder());

		factor = Math.max(1, cfg.getBufferFrames() / jump3r.getFrameSize());
		mp3Buffer = ByteBuffer.allocateDirect(factor * jump3r.getFrameSize() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.nativeOrder());
	}

	@Override
	public void write() throws InterruptedException {
		((Buffer) mp3Buffer).clear();
		boolean decoded = true;
		for (int i = 0; i < factor; i++) {
			((Buffer) decodedMP3Buffer).clear();
			decoded &= jump3r.decode(decodedMP3Buffer);
			if (!decoded) {
				break;
			}
			if (jump3r.getChannels() == 1) {
				monoToStereo(decodedMP3Buffer, mp3Buffer);
			} else {
				mp3Buffer.put(decodedMP3Buffer);
			}
		}
		if (audioSection.isPlayOriginal()) {
			((Buffer) buffer()).clear();
			((Buffer) mp3Buffer).flip();
			buffer().put(mp3Buffer);
		}
		super.write();
		if (!decoded) {
			throw new MP3Termination();
		}
	}

	@Override
	public void close() {
		super.close();
		if (jump3r != null) {
			jump3r.close();
		}
	}

	private void monoToStereo(ByteBuffer monoMP3Buffer, ByteBuffer stereoBuffer) {
		ShortBuffer monoBuffer = monoMP3Buffer.asShortBuffer();
		for (int i = 0; i < monoBuffer.capacity(); i++) {
			short monoSample = monoBuffer.get();
			stereoBuffer.putShort(monoSample);
			stereoBuffer.putShort(monoSample);
		}
	}
}
