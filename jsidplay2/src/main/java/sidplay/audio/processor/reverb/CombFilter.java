// Comb Filter with sustain Class
// Written by: Craig A. Lindley
// Last Update: 09/09/98

package sidplay.audio.processor.reverb;

public class CombFilter {

	private int sampleBufferSize;
	private int sampleRate;
	private int numberOfChannels;
	private double delayInMs;
	private double sustainTimeInMs;
	private double gain;
	private int sustainSampleCount;
	private double[] delayBuffer;
	private int delayBufferSize;
	private int writeIndex;
	private int readIndex;

	public CombFilter(int sampleRate, int numberOfChannels, double delayInMs, int sampleBufferSize) {
		// Save incoming
		this.sampleRate = sampleRate;
		this.numberOfChannels = numberOfChannels;
		this.sampleBufferSize = sampleBufferSize;

		// Set some defaults
		gain = 0.0;
		sustainTimeInMs = 500;

		// Initialize delay parameters
		setDelayInMs(delayInMs);
	}

	public double getDelayInMs() {
		return delayInMs;
	}

	public void setDelayInMs(double delayInMs) {
		this.delayInMs = delayInMs;

		// Do calculation to determine delay buffer size
		int delayOffset = (int) ((delayInMs + 0.5) * sampleRate * numberOfChannels) / 1000;

		delayBufferSize = sampleBufferSize + delayOffset;

		// Allocate new delay buffer
		delayBuffer = new double[delayBufferSize];

		// Initialize indices
		// Index where dry sample is written
		writeIndex = 0;

		// Index where wet sample is read
		readIndex = sampleBufferSize;

		// Calculate gain
		calcGain();
	}

	public void calcGain() {
		// Calculate gain for this filter such that a recirculating
		// sample will reduce in level 60db in the specified
		// sustain time.
		gain = Math.pow(0.001, delayInMs / sustainTimeInMs);
	}

	public double getSustainTimeInMs() {
		return sustainTimeInMs;
	}

	// Sustain time is the time it takes the signal to drop
	// approximately 60 db (1/1000th) in level.
	public void setSustainTimeInMs(double sustainTimeInMs) {

		this.sustainTimeInMs = sustainTimeInMs;

		// Number of samples needed for sustain duration
		sustainSampleCount = (int) ((sustainTimeInMs * sampleRate * numberOfChannels) / 1000);

		// Calculate gain for this filter
		calcGain();
	}

	// Do the data processing
	public int doFilter(short[] inBuf, double[] outBuf, int length) {

		// See if at end of data
		if (length != -1) {
			// There are input samples so sustain is not in effect
			for (int i = 0; i < length; i++) {
				double sample = inBuf[i];
				double delaySample = delayBuffer[readIndex++];

				// Output is from delay buffer
				outBuf[i] += delaySample;

				// Apply gain and feedback to sample
				sample += delaySample * gain;

				// Store sample in delay buffer
				delayBuffer[writeIndex++] = sample;

				// Update buffer indices
				if (readIndex == delayBufferSize) {
					readIndex = 0;
				}
				if (writeIndex == delayBufferSize) {
					writeIndex = 0;
				}
			}
			return length;

		} else {

			// No more input samples are available therefore sustain
			// mode is in effect.
			int samplesToMove = Math.min(outBuf.length, sustainSampleCount);
			if (samplesToMove <= 0)
				return -1;

			// Move the sustain samples
			for (int i = 0; i < samplesToMove; i++) {
				double delaySample = delayBuffer[readIndex++];

				// Output is from delay buffer
				outBuf[i] += delaySample;

				// Apply gain and feedback to sample
				delaySample *= gain;

				// Store sample in delay buffer
				delayBuffer[writeIndex++] = delaySample;

				// Update buffer indices
				if (readIndex == delayBufferSize) {
					readIndex = 0;
				}
				if (writeIndex == delayBufferSize) {
					writeIndex = 0;
				}
				sustainSampleCount--;
			}
			return samplesToMove;
		}
	}

}
