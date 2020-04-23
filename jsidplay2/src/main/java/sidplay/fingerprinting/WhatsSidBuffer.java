package sidplay.fingerprinting;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import sidplay.audio.WAVHeader;

/**
 * WhatsSid buffer containing always the last N seconds of sound samples in a
 * ring buffer.
 * 
 * @author ken
 *
 */
public final class WhatsSidBuffer {

	/**
	 * Number of channels.
	 */
	private static final int CHANNELS = 2;

	/**
	 * WhatsSid resampler to 8Khz.
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
	 * WhatsSid capture buffer.
	 */
	private ByteBuffer whatsSidBuffer;

	/**
	 * Capacity of the WhatsSid buffer.
	 */
	private int whatsSidBufferSize;

	/**
	 * WhatsSid? Last match
	 */
	private static MusicInfoWithConfidenceBean lastWhatsSidMatch;

	/**
	 * WhatsSid? minimum confidence to detect a match
	 */
	private double minimumRelativeConfidence;

	/**
	 * WhatsSid buffer WAV sample data
	 */
	public byte[] whatsSidBufferSamples;

	public WhatsSidBuffer(double cpuFrequency, int captureTimeInS, double minimumRelativeConfidence) {
		this.downSamplerL = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.downSamplerR = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.whatsSidBufferSize = Short.BYTES * CHANNELS * SamplingRate.VERY_LOW.getFrequency() * captureTimeInS;
		this.whatsSidBuffer = ByteBuffer.allocateDirect(whatsSidBufferSize).order(ByteOrder.LITTLE_ENDIAN);
		this.whatsSidBufferSamples = new byte[0];
		this.minimumRelativeConfidence = minimumRelativeConfidence;
	}

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
				whatsSidBufferSamples = WhatsSidBufferSamples();
				((Buffer) whatsSidBuffer).clear();
				return true;
			}
		}
		return false;
	}

	public byte[] getWhatsSidBufferSamples() {
		return whatsSidBufferSamples;
	}

	public void clear() {
		((Buffer) whatsSidBuffer).clear();
		((Buffer) whatsSidBuffer.put(new byte[whatsSidBufferSize])).clear();
		init();
	}

	public void init() {
		lastWhatsSidMatch = null;
	}

	public boolean match(MusicInfoWithConfidenceBean result) {
		if (result != null && !result.equals(lastWhatsSidMatch) && result.getRelativeConfidence() > minimumRelativeConfidence) {
			lastWhatsSidMatch = result;
			return true;
		}
		return false;
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

	private byte[] WhatsSidBufferSamples() {
		byte[] result = new byte[WAVHeader.HEADER_LENGTH + whatsSidBufferSize];
		WAVHeader wavHeader = new WAVHeader(CHANNELS, SamplingRate.VERY_LOW.getFrequency());
		wavHeader.advance(whatsSidBufferSize);
		System.arraycopy(wavHeader.getBytes(), 0, result, 0, wavHeader.getBytes().length);
		((Buffer) whatsSidBuffer).rewind();
		whatsSidBuffer.get(result, WAVHeader.HEADER_LENGTH, whatsSidBufferSize);
		return result;
	}

}
