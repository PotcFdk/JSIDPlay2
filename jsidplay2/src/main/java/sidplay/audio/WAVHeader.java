package sidplay.audio;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * WAV header format.
 * 
 * @author Ken Händel
 */
public class WAVHeader {

	private static final int HEADER_OFFSET = 8;
	public static final int HEADER_LENGTH = 44;

	private int length, sampleFreq,bytesPerSec, dataChunkLen;
	private short format, channels, blockAlign, bitsPerSample;

	public WAVHeader(int channels, int frameRate) {
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

}