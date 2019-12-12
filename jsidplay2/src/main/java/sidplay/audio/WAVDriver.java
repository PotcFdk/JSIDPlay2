package sidplay.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;

/**
 * File based driver to create a WAV file.
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
			file = new RandomAccessFile(new File(recordingFilename), "rw");
			return new FileOutputStream(file.getFD());
		}

		@Override
		public void close() {
			super.close();
			if (out != null) {
				try {
					file.seek(0);

					writeHeader();
					out.close();

					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Driver to write into an WAV output stream.<BR>
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class WAVStream extends WAVDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 * 
		 * @param out Output stream to write the encoded MP3 to
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
	static class WavHeader {
		private static final Charset US_ASCII = Charset.forName("US-ASCII");

		private int length;
		private short format;
		private short channels;
		private int sampleFreq;
		private int bytesPerSec;
		private short blockAlign;
		private short bitsPerSample;
		private int dataChunkLen;

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

	private static final int HEADER_LENGTH = 44;
	private static final int HEADER_OFFSET = 8;

	/**
	 * Output stream to write the encoded MP3 to.
	 */
	protected OutputStream out;

	private ByteBuffer sampleBuffer;

	private int samplesWritten;

	private final WavHeader wavHdr = new WavHeader();

	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		final int blockAlign = Short.BYTES * cfg.getChannels();

		out = getOut(recordingFilename);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * blockAlign);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);

		wavHdr.length = HEADER_LENGTH - HEADER_OFFSET;
		wavHdr.format = 1;
		wavHdr.channels = (short) cfg.getChannels();
		wavHdr.sampleFreq = cfg.getFrameRate();
		wavHdr.bytesPerSec = cfg.getFrameRate() * blockAlign;
		wavHdr.blockAlign = (short) blockAlign;
		wavHdr.bitsPerSample = 16;

		out.write(wavHdr.getBytes());

		samplesWritten = 0;
	}

	@Override
	public void write() throws InterruptedException {
		try {
			int len = sampleBuffer.position();
			out.write(sampleBuffer.array(), 0, len);
			samplesWritten += len;
		} catch (final IOException e) {
			e.printStackTrace();
			throw new InterruptedException();
		}
	}

	@Override
	public void close() {
	}

	protected void writeHeader() throws IOException {
		wavHdr.length = samplesWritten + HEADER_LENGTH - HEADER_OFFSET;
		wavHdr.dataChunkLen = samplesWritten;
		out.write(wavHdr.getBytes(), 0, HEADER_LENGTH);
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
