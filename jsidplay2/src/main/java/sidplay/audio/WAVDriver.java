package sidplay.audio;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;

/**
 * Abstract base class to output a WAV to an output stream.
 * 
 * @author Ken Händel
 * 
 */
public abstract class WAVDriver implements AudioDriver {

	/**
	 * File based driver to create a WAV file.
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class WavFile extends WAVDriver {

		private RandomAccessFile file;

		@Override
		protected OutputStream getOut(String recordingFilename) throws IOException {
			System.out.println("Recording, file=" + recordingFilename);
			file = new RandomAccessFile(recordingFilename, "rw");
			return new FileOutputStream(file.getFD());
		}

		@Override
		public void close() {
			super.close();
			if (out != null) {
				try {
					file.seek(0);
					out.write(wavHeader.getBytes());
					out.close();

					file.close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing WAV audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an WAV output stream.<BR>
	 * 
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class WAVStream extends WAVDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 * 
		 * @param out Output stream to write the encoded WAV to
		 */
		public WAVStream(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	/**
	 * WAV header format.
	 * 
	 * @author Ken Händel
	 */
	private static class WavHeader {

		private static final int HEADER_OFFSET = 8;
		private static final int HEADER_LENGTH = 44;

		private int length;
		private short format;
		private short channels;
		private int sampleFreq;
		private int bytesPerSec;
		private short blockAlign;
		private short bitsPerSample;
		private int dataChunkLen;

		public WavHeader(int channels, int frameRate) {
			this.length = HEADER_LENGTH - HEADER_OFFSET;
			this.format = 1;
			this.channels = (short) channels;
			this.sampleFreq = frameRate;
			this.bytesPerSec = frameRate * Short.BYTES * channels;
			this.blockAlign = (short) (Short.BYTES * channels);
			this.bitsPerSample = 16;
			this.dataChunkLen = 0;
		}

		public void advance(int length) {
			this.length += length;
			this.dataChunkLen += length;
		}

		private byte[] getBytes() {
			final ByteBuffer b = ByteBuffer.allocate(HEADER_LENGTH);
			b.order(ByteOrder.LITTLE_ENDIAN);
			b.put("RIFF".getBytes(US_ASCII));
			b.putInt(length);
			b.put("WAVE".getBytes(US_ASCII));
			b.put("fmt ".getBytes(US_ASCII));
			b.putInt(16);
			b.putShort(format);
			b.putShort(channels);
			b.putInt(sampleFreq);
			b.putInt(bytesPerSec);
			b.putShort(blockAlign);
			b.putShort(bitsPerSample);
			b.put("data".getBytes(US_ASCII));
			b.putInt(dataChunkLen);
			return b.array();
		}

	}

	/**
	 * Output stream to write the encoded WAV to.
	 */
	protected OutputStream out;

	protected WavHeader wavHeader;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		wavHeader = new WavHeader(cfg.getChannels(), cfg.getFrameRate());

		out = getOut(recordingFilename);
		out.write(wavHeader.getBytes());

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			out.write(sampleBuffer.array(), 0, sampleBuffer.position());
			wavHeader.advance(sampleBuffer.position());
		} catch (final IOException e) {
			throw new RuntimeException("Error writing WAV audio stream", e);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	@Override
	public String getExtension() {
		return ".wav";
	}
	
	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
