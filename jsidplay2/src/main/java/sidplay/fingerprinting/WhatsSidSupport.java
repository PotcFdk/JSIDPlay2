package sidplay.fingerprinting;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import sidplay.audio.WAVHeader;

/**
 * Use a WhatsSid capture buffer to match a currently played tune. Capture
 * buffer contains always the last N seconds of sound samples in a ring buffer.
 * 
 * @author ken
 *
 */
public final class WhatsSidSupport {

	/**
	 * Number of channels.
	 */
	private static final int CHANNELS = 2;

	/**
	 * Resampler to 8Khz.
	 */
	private final Resampler downSamplerL, downSamplerR;

	/**
	 * Random source for triangular dithering
	 */
	private final Random RANDOM = new Random();
	/**
	 * State of HP-TPDF.
	 */
	private int oldRandomValue;

	/**
	 * Capture buffer.
	 */
	private ByteBuffer whatsSidBuffer;

	/**
	 * Capacity of the capture buffer.
	 */
	private int whatsSidBufferSize;

	/**
	 * Last match
	 */
	private static MusicInfoWithConfidenceBean lastWhatsSidMatch;

	/**
	 * Minimum confidence to detect a match
	 */
	private double minimumRelativeConfidence;

	/**
	 * WAV sample data to match
	 */
	public byte[] whatsSidBufferSamples;

	public WhatsSidSupport(double cpuFrequency, int captureTimeInS, double minimumRelativeConfidence) {
		this.downSamplerL = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.downSamplerR = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.whatsSidBufferSize = Short.BYTES * CHANNELS * SamplingRate.VERY_LOW.getFrequency() * captureTimeInS;
		this.whatsSidBuffer = ByteBuffer.allocateDirect(whatsSidBufferSize).order(ByteOrder.LITTLE_ENDIAN);
		this.whatsSidBufferSamples = new byte[0];
		this.minimumRelativeConfidence = minimumRelativeConfidence;
	}

	/**
	 * Output sample data to fill WhatsSid capture buffer
	 * 
	 * @param valL left channel sample data
	 * @param valR right channel sample data
	 * @return capture buffer full
	 */
	public boolean output(int valL, int valR) {
		int dither = triangularDithering();
		if (downSamplerL.input(valL)) {
			whatsSidBuffer.putShort(
					(short) Math.max(Math.min(downSamplerL.output() + dither, Short.MAX_VALUE), Short.MIN_VALUE));
		}
		if (downSamplerR.input(valR)) {
			if (!whatsSidBuffer.putShort(
					(short) Math.max(Math.min(downSamplerR.output() + dither, Short.MAX_VALUE), Short.MIN_VALUE))
					.hasRemaining()) {
				whatsSidBufferSamples = createWhatsSidBufferSamples();
				((Buffer) whatsSidBuffer).clear();
				return true;
			}
		}
		return false;
	}

	/**
	 * Match WhatsSid capture buffer using the given matcher
	 * 
	 * @param matcher matcher used to match the capture buffer contents
	 * @return matched music info or null (no match)
	 * @throws IOException I/O error
	 */
	public MusicInfoWithConfidenceBean match(IFingerprintMatcher matcher) throws IOException {
		if (whatsSidBufferSamples.length > 0) {
			MusicInfoWithConfidenceBean result = matcher.match(new WavBean(whatsSidBufferSamples));
			if (result != null && !result.equals(lastWhatsSidMatch)
					&& result.getRelativeConfidence() > minimumRelativeConfidence) {
				lastWhatsSidMatch = result;
				return result;
			}
		}
		return null;
	}

	/**
	 * Reset the capture buffer (call for a new tune to play)
	 */
	public void reset() {
		((Buffer) whatsSidBuffer).clear();
		((Buffer) whatsSidBuffer.put(new byte[whatsSidBufferSize])).clear();
		lastWhatsSidMatch = null;
	}

	/**
	 * Triangularly shaped noise source for audio applications. Output of this PRNG
	 * is between ]-1, 1[.
	 * 
	 * @return triangular noise sample
	 */
	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x1;
		return oldRandomValue - prevValue;
	}

	private byte[] createWhatsSidBufferSamples() {
		byte[] result = new byte[WAVHeader.HEADER_LENGTH + whatsSidBufferSize];
		WAVHeader wavHeader = new WAVHeader(CHANNELS, SamplingRate.VERY_LOW.getFrequency());
		wavHeader.advance(whatsSidBufferSize);
		System.arraycopy(wavHeader.getBytes(), 0, result, 0, wavHeader.getBytes().length);
		((Buffer) whatsSidBuffer).rewind();
		whatsSidBuffer.get(result, WAVHeader.HEADER_LENGTH, whatsSidBufferSize);
		return result;
	}

}
