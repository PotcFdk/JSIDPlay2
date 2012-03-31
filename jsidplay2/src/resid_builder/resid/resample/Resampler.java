package resid_builder.resid.resample;

/**
 * Abstraction of a resampling process. Given enough input, produces output.
 * Cnstructors take additional arguments that configure these objects.
 * 
 * @author Antti Lankila
 */
public interface Resampler {
	/**
	 * Input a sample into resampler. Output "true" when resampler is ready with new sample.
	 * 
	 * @param sample
	 * @return true when a sample is ready
	 */
	boolean input(int sample);
	
	/**
	 * Output a sample from resampler
	 * 
	 * @return resampled sample
	 */
	int output();

	void reset();
}
