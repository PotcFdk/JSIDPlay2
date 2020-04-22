package sidplay.fingerprinting;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
	 * WhatsSid capture buffer.
	 */
	private ByteBuffer whatsSidBuffer;

	/**
	 * Capacity of the WhatsSid buffer.
	 */
	private int whatsSidBufferSize;

	public WhatsSidBuffer(double cpuFrequency, int captureTimeInS) {
		this.downSamplerL = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.downSamplerR = Resampler.createResampler(cpuFrequency, SamplingMethod.RESAMPLE,
				SamplingRate.VERY_LOW.getFrequency(), SamplingRate.VERY_LOW.getMiddleFrequency());
		this.whatsSidBufferSize = Short.BYTES * CHANNELS * SamplingRate.VERY_LOW.getFrequency() * captureTimeInS;
		this.whatsSidBuffer = ByteBuffer.allocateDirect(whatsSidBufferSize).order(ByteOrder.LITTLE_ENDIAN);
	}

	public boolean output(int valL, int valR, int dither) {
		if (downSamplerL.input(valL)) {
			whatsSidBuffer.putShort(
					(short) Math.max(Math.min(downSamplerL.output() + dither, Short.MAX_VALUE), Short.MIN_VALUE));
		}
		if (downSamplerR.input(valR)) {
			if (!whatsSidBuffer.putShort(
					(short) Math.max(Math.min(downSamplerR.output() + dither, Short.MAX_VALUE), Short.MIN_VALUE))
					.hasRemaining()) {
				((Buffer) whatsSidBuffer).flip();
				return true;
			}
		}
		return false;
	}

	public byte[] getWAV() {
		if (whatsSidBuffer == null) {
			return new byte[0];
		}
		ByteBuffer copy = whatsSidBuffer.asReadOnlyBuffer();
		ByteBuffer result = ByteBuffer.allocate(WAVHeader.HEADER_LENGTH + whatsSidBufferSize)
				.order(ByteOrder.LITTLE_ENDIAN);
		WAVHeader wavHeader = new WAVHeader(CHANNELS, SamplingRate.VERY_LOW.getFrequency());
		wavHeader.advance(whatsSidBufferSize);
		result.put(wavHeader.getBytes());
		((Buffer) copy).mark();
		result.put(copy);
		((Buffer) copy).reset();
		((Buffer) copy).flip();
		result.put(copy);
		((Buffer) copy).limit(whatsSidBufferSize);
		return result.array();
	}

	public void clear() {
		if (whatsSidBuffer == null) {
			return;
		}
		((Buffer) whatsSidBuffer).clear();
		((Buffer) whatsSidBuffer.put(new byte[whatsSidBufferSize])).clear();
	}

}
