package sidplay.audio;

import java.io.FileOutputStream;
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
 * Abstract base class to output an MP3 encoded tune to an output stream.
 * 
 * @author Ken Händel
 * 
 */
public abstract class MP3Driver extends AudioDriver {

	/**
	 * File based driver to create a MP3 file.
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class MP3File extends MP3Driver {
		@Override
		protected OutputStream getOut(SidTune tune) throws IOException {
			return new FileOutputStream(recordingFilenameProvider.apply(tune)
					+ ".mp3");
		}
	}

	/**
	 * Driver to write into an MP3 encoded output stream.<BR>
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class MP3Stream extends MP3Driver {

		/**
		 * Output stream to write the encoded MP3 to.
		 */
		private OutputStream out;

		/**
		 * Use several instances for parallel emulator instances, where
		 * applicable.
		 * 
		 * @param out
		 *            Output stream to write the encoded MP3 to
		 */
		public MP3Stream(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(SidTune tune) {
			return out;
		}
	}

	/**
	 * Sample buffer to be encoded as MP3.
	 */
	protected ByteBuffer sampleBuffer;
	/**
	 * MP3: Constant bit rate (-1=auto)
	 */
	protected int cbr = LameEncoder.DEFAULT_BITRATE;
	/**
	 * MP3: Variable bit rate quality (0=best, 5=medium, 9=worst)
	 */
	protected int vbrQuality = LameEncoder.DEFAULT_QUALITY;
	/**
	 * Use variable bit rate mode? (or constant bit rate mode)
	 */
	protected boolean vbr = LameEncoder.DEFAULT_VBR;

	/**
	 * Output stream to write the encoded MP3 to.
	 */
	private OutputStream out;
	/**
	 * Jump3r encoder.
	 */
	private LameEncoder jump3r;

	public void setCbr(int cbr) {
		this.cbr = cbr;
	}

	public void setVbrQuality(int vbrQuality) {
		this.vbrQuality = vbrQuality;
	}

	public void setVbr(boolean isVbr) {
		this.vbr = isVbr;
	}

	@Override
	public void open(final AudioConfig cfg, SidTune tune)
			throws LineUnavailableException, UnsupportedAudioFileException,
			IOException {
		final int blockAlign = Short.BYTES * cfg.channels;

		// We need to make a buffer for the user
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * blockAlign);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);
		AudioFormat audioFormat = new AudioFormat(cfg.frameRate, Short.SIZE,
				cfg.channels, true, false);
		jump3r = new LameEncoder(audioFormat, cbr, MPEGMode.STEREO, vbrQuality,
				vbr);
		out = getOut(tune);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			byte[] encoded = new byte[jump3r.getMP3BufferSize()];
			int bytesWritten = jump3r.encodeBuffer(sampleBuffer.array(), 0,
					sampleBuffer.capacity(), encoded);
			out.write(encoded, 0, bytesWritten);
		} catch (ArrayIndexOutOfBoundsException | IOException e) {
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
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	protected abstract OutputStream getOut(SidTune tune) throws IOException;
}
