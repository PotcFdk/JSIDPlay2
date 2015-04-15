package resid_builder.resample;

import libsidplay.common.SamplingMethod;

/**
 * Abstraction of a resampling process. Given enough input, produces output.
 * Constructors take additional arguments that configure these objects.
 * 
 * @author Antti Lankila
 */
public interface Resampler {
	/**
	 * Input a sample into resampler. Output "true" when resampler is ready with
	 * new sample.
	 * 
	 * @param sample
	 *            The sample to input into the resampler.
	 * @return true when a sample is ready
	 */
	boolean input(int sample);

	/**
	 * Output a sample from resampler
	 * 
	 * @return resampled sample
	 */
	int output();

	/**
	 * Resets this resampler.
	 */
	void reset();

	/**
	 * Setting of SID sampling parameters.
	 * <P>
	 * Use a clock freqency of 985248Hz for PAL C64, 1022730Hz for NTSC C64. The
	 * default end of passband frequency is pass_freq = 0.9*sample_freq/2 for
	 * sample frequencies up to ~ 44.1kHz, and 20kHz for higher sample
	 * frequencies.
	 * <P>
	 * For resampling, the ratio between the clock frequency and the sample
	 * frequency is limited as follows: 125*clock_freq/sample_freq < 16384 E.g.
	 * provided a clock frequency of ~ 1MHz, the sample frequency can not be set
	 * lower than ~ 8kHz. A lower sample frequency would make the resampling
	 * code overfill its 16k sample ring buffer.
	 * <P>
	 * The end of passband frequency is also limited: pass_freq <=
	 * 0.9*sample_freq/2
	 * <P>
	 * E.g. for a 44.1kHz sampling rate the end of passband frequency is limited
	 * to slightly below 20kHz. This constraint ensures that the FIR table is
	 * not overfilled.
	 * 
	 * @param clockFrequency
	 *            System clock frequency at Hz
	 * @param method
	 *            sampling method to use
	 * @param samplingFrequency
	 *            Desired output sampling rate
	 */
	static Resampler createResampler(final double clockFrequency,
			final SamplingMethod method, final double samplingFrequency,
			double highestAccurateFrequency) {
		switch (method) {
		case DECIMATE:
			return new ZeroOrderResampler(clockFrequency, samplingFrequency);
		case RESAMPLE:
			return new TwoPassSincResampler(clockFrequency, samplingFrequency,
					highestAccurateFrequency);
		default:
			throw new RuntimeException("Unknown sampling method: " + method);
		}
	}

}
