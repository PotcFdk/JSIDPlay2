package sidplay.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.sidtune.SidTune;
import lowlevel.LameEncoder;
import mp3.MPEGMode;

/**
 * Driver to write into an MP3 encoded output stream.
 * 
 * @author Ken Händel
 * 
 */
public class MP3Stream extends AudioDriver {

	/**
	 * Sample buffer to be encoded as MP3.
	 */
	private ByteBuffer sampleBuffer;
	/**
	 * Output stream to write the encoded MP3 to.
	 */
	private OutputStream out;
	/**
	 * Jump3r encoder.
	 */
	private LameEncoder jump3r;

	/**
	 * MP3: Constant bit rate (-1=auto)
	 */
	private int cbr;
	/**
	 * MP3: Variable bit rate quality (0=best, 5=medium, 9=worst)
	 */
	private int vbr;
	/**
	 * Use variable bit rate mode? (or constant bit rate mode)
	 */
	private boolean isVbr;

	public MP3Stream(OutputStream out, int cbr, int vbr, boolean isVbr) {
		this.out = out;
		this.cbr = cbr;
		this.vbr = vbr;
		this.isVbr = isVbr;
	}

	@Override
	public void open(final AudioConfig cfg, SidTune tune)
			throws LineUnavailableException, UnsupportedAudioFileException,
			IOException {
		final int channels = cfg.channels;
		final int blockAlign = Short.BYTES * channels;

		// We need to make a buffer for the user
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * blockAlign);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);
		AudioFormat audioFormat = new AudioFormat(cfg.frameRate, Short.SIZE,
				channels, true, false);
		jump3r = new LameEncoder(audioFormat, cbr, MPEGMode.STEREO, vbr, isVbr);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			byte[] encoded = new byte[jump3r.getMP3BufferSize()];
			int bytesWritten = jump3r.encodeBuffer(sampleBuffer.array(), 0,
					sampleBuffer.capacity(), encoded);
			out.write(encoded, 0, bytesWritten);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			throw new InterruptedException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InterruptedException();
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void close() {
		if (jump3r != null) {
			jump3r.close();
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

}
