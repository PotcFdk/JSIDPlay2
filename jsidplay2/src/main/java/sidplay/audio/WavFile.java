package sidplay.audio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import libsidplay.sidtune.SidTune;

/**
 * File based driver to create a WAV file.
 * 
 * @author Ken Händel
 * 
 *         Only unsigned 8-bit, and signed 16-bit, samples are supported.
 *         Endian-ess is adjusted if necessary.
 * 
 *         If number of sample bytes is given, this can speed up the process of
 *         closing a huge file on slow storage media.
 * 
 */
public class WavFile extends AudioDriver {
	protected static final Charset US_ASCII = Charset.forName("US-ASCII");

	private static final String EXTENSION = ".wav";

	private ByteBuffer sampleBuffer;

	/**
	 * @author Ken Händel
	 * 
	 *         little endian format
	 */
	static class WavHeader {
		public static final int HEADER_LENGTH = 44;

		public byte[] getBytes() {
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

		protected int length;
		protected short format;
		protected short channels;
		protected int sampleFreq;
		protected int bytesPerSec;
		protected short blockAlign;
		protected short bitsPerSample;
		protected int dataChunkLen;
	}

	private int byteCount;

	private final WavHeader wavHdr = new WavHeader();
	private RandomAccessFile file;

	@Override
	public void open(final AudioConfig cfg, SidTune tune) throws IOException {
		final int channels = cfg.channels;
		final int freq = cfg.frameRate;
		final int blockAlign = 2 * channels;
		byteCount = 0;

		// We need to make a buffer for the user
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * blockAlign);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);

		// Fill in header with parameters and expected file size.
		wavHdr.length = WavHeader.HEADER_LENGTH - 8;
		wavHdr.format = 1;
		wavHdr.channels = (short) channels;
		wavHdr.sampleFreq = freq;
		wavHdr.bytesPerSec = freq * blockAlign;
		wavHdr.blockAlign = (short) blockAlign;
		wavHdr.bitsPerSample = 16;

		file = new RandomAccessFile(recordingFilenameProvider.getFilename(tune)
				+ EXTENSION, "rw");
		file.setLength(0);
		file.write(wavHdr.getBytes());
	}

	/**
	 * After write call old buffer is invalid and you should use the new buffer
	 * provided instead.
	 */
	@Override
	public void write() {
		byteCount += sampleBuffer.capacity();
		try {
			file.write(sampleBuffer.array(), 0, sampleBuffer.capacity());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void close() {
		try {
			wavHdr.length = byteCount + WavHeader.HEADER_LENGTH - 8;
			wavHdr.dataChunkLen = byteCount;
			file.seek(0);
			file.write(wavHdr.getBytes(), 0, WavHeader.HEADER_LENGTH);
			file.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

}
