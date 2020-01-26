// Allpass Network Class
// Written by: Craig A. Lindley
// Last Update: 09/12/98

package sidplay.audio.processor.reverb;

public class AllpassNetwork {

	private int SAMPLEBUFFERSIZE;
	public AllpassNetwork(int sampleRate, int numberOfChannels,
						  double delayInMs, int SAMPLEBUFFERSIZE) {

		// Save incoming
		this.sampleRate = sampleRate;
		this.numberOfChannels = numberOfChannels;
		this.SAMPLEBUFFERSIZE = SAMPLEBUFFERSIZE;

		// Default gain of allpass network
		double networkGain = 0.7;
		gain1 = -networkGain;
		gain2 = 1.0 - (networkGain * networkGain);
		gain3 = networkGain;

		// Default sustain
		sustainTimeInMs = 65.0;

		// Initialize delay parameters
		setDelayInMs(delayInMs);
	}

	public void setDelayInMs(double delayInMs) {

		this.delayInMs = delayInMs;

		// Do calculation to determine delay buffer size
		int delayOffset = (int)
				((delayInMs + 0.5) * sampleRate * numberOfChannels) / 1000;

		delayBufferSize = SAMPLEBUFFERSIZE + delayOffset;

		// Allocate new delay buffer
		delayBuffer = new double[delayBufferSize];

		// Initialize indices
		// Index where dry sample is written
		writeIndex = 0;
			
		// Index where wet sample is read
		readIndex = SAMPLEBUFFERSIZE;

		// Calculate gain for filter
		calcGain();
	}
		
	public double getDelayInMs() {
		return delayInMs;
	}
	
	private void calcGain() {

		// Calculate gain for this filter such that a recirculating
		// sample will reduce in level 60db in the specified
		// sustain time.
		double gain = Math.pow(0.001, delayInMs / sustainTimeInMs);

		// Now update the network gain
		gain1 = -gain;
		gain2 = 1.0 - (gain * gain);
		gain3 = gain;
	}
	
	// Sustain time is the time it takes the signal to drop
	// approximately 60 db (1/1000th) in level.
	public void setSustainTimeInMs(double sustainTimeInMs) {

		this.sustainTimeInMs = sustainTimeInMs;

		// Number of samples needed for sustain duration
		sustainSampleCount = 
			(int) ((sustainTimeInMs * sampleRate * numberOfChannels) / 1000);

		// Calculate gain for this filter
		calcGain();
	}

	public double getSustainTimeInMs() {
		return sustainTimeInMs;
	}
	
	// Do the data processing
	public int doFilter(double [] inBuf, double [] outBuf, int length) {

		// See if at end of data
		if (length != -1) {
			// Sustain is not in effect because there are input samples
			for (int i=0; i < length; i++) {
				double inSample = inBuf[i];
				double outSample = inSample * gain1;
				double delaySample = delayBuffer[readIndex++];
				outSample += delaySample * gain2;
				
				// Output the new sample
				outBuf[i] = outSample;

				// Apply gain and feedback to sample
				inSample += delaySample * gain3;
				
				// Store sample in delay buffer
				delayBuffer[writeIndex++] = inSample;

				// Update buffer indices
				readIndex  %= delayBufferSize;
				writeIndex %= delayBufferSize;
			}
			return length;
		
		}	else	{
		
			// No more input samples are available therefore sustain is in
			// mode is in effect.
			int samplesToMove = Math.min(outBuf.length, sustainSampleCount);
			if (samplesToMove <= 0)
				return -1;

			// No more input samples are available therefore sustain is in
			// mode is in effect.
			for (int i=0; i < samplesToMove; i++) {
				double delaySample = delayBuffer[readIndex++];
				double outSample = delaySample * gain2;
				
				// Output is from delay buffer
				outBuf[i] = outSample;

				// Apply gain and feedback to sample
				double inSample = delaySample * gain3;
				
				// Store sample in delay buffer
				delayBuffer[writeIndex++] = inSample;

				// Update buffer indices
				readIndex  %= delayBufferSize;
				writeIndex %= delayBufferSize;
				sustainSampleCount--;
			}
			return samplesToMove;
		}
	}

	// Private class data
	private int sampleRate;
	private int numberOfChannels;
	private double delayInMs;
	private double sustainTimeInMs;
	private double gain1;
	private double gain2;
	private double gain3;
	private int sustainSampleCount;
	private double [] delayBuffer;
	private int delayBufferSize;
	private int writeIndex;
	private int readIndex;
}


