package sidplay.fingerprinting;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidutils.fingerprinting.IFingerprintMatcher;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import sidplay.audio.wav.WAVHeader;

/**
 * 
 * WhatsSID? client part of tune recognition.
 * 
 * Use a WhatsSID capture buffer to match a currently played tune. Capture
 * buffer contains always the last N seconds of sound samples in a ring buffer.
 *
 * @author ken
 *
 */
public final class WhatsSidSupport {

	/**
	 * Number of channels.
	 */
	private static final int CHANNELS = 1;

	/**
	 * Resampler to 8Khz.
	 */
	private final Resampler downSampler;

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

	public WhatsSidSupport(double cpuFrequency, int captureTimeInS, double minimumRelativeConfidence) {
		this.downSampler = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.whatsSidBufferSize = Short.BYTES * CHANNELS * SamplingRate.VERY_LOW.getFrequency() * captureTimeInS;
		this.whatsSidBuffer = ByteBuffer.allocateDirect(whatsSidBufferSize).order(ByteOrder.LITTLE_ENDIAN);
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
		if (downSampler.input(valL + valR >> 1)) {
			if (!whatsSidBuffer
					.putShort(
							(short) Math.max(Math.min(downSampler.output() + dither, Short.MAX_VALUE), Short.MIN_VALUE))
					.hasRemaining()) {
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
		byte[] wav = createWAV();
		if (wav.length > 0) {
			MusicInfoWithConfidenceBean result = matcher.match(new WAVBean(wav));
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
		resetLastWhatsSidMatch();
	}

	private static void resetLastWhatsSidMatch() {
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

	/**
	 * Create WAV sample data form ring-buffer.
	 *
	 * @return WAV bytes
	 */
	private byte[] createWAV() {
		ByteBuffer result = ByteBuffer.allocate(WAVHeader.HEADER_LENGTH + whatsSidBufferSize)
				.order(ByteOrder.LITTLE_ENDIAN);
		WAVHeader wavHeader = new WAVHeader(CHANNELS, SamplingRate.VERY_LOW.getFrequency());
		wavHeader.advance(whatsSidBufferSize);
		result.put(wavHeader.getBytes());
		ByteBuffer copy = whatsSidBuffer.asReadOnlyBuffer();
		((Buffer) copy).mark();
		result.put(copy);
		((Buffer) copy).reset();
		((Buffer) copy).flip();
		result.put(copy);
		((Buffer) copy).limit(whatsSidBufferSize);
		return result.array();
	}

}
