// Schroeder Reverb Class
// Written by: Craig A. Lindley
// Last Update: 09/11/98

package sidplay.audio.processor.reverb;

/*
This reverb module is called a Schroeder reverb because
the organization of parallel comb filters and series connected
allpass filters was suggested by M.R. Schroeder. See the book, 
"Computer Music -- Synthesis, Composition and Performance" by 
Charles Dodge and Thomas Jerse for details. 
*/

public class SchroederReverb {

	// Parameters below were chosen to simulate the characteristics of
	// a medium-sized concert hall. See book quoted above for details.
	public static final double COMB1DELAYMSDEF = 29.7;
	public static final double COMB2DELAYMSDEF = 37.1;
	public static final double COMB3DELAYMSDEF = 41.1;
	public static final double COMB4DELAYMSDEF = 43.7;

	public static final double ALLPASS1DELAYMSDEF = 5.0;
	public static final double ALLPASS2DELAYMSDEF = 1.7;
	
	public static final double ALLPASS1SUSTAINMSDEF = 96.8;
	public static final double ALLPASS2SUSTAINMSDEF = 32.9;
	
	
	
	private static final double SUSTAINTIMEMSDEF = 500;
	private static final double MIXDEF = 0.25;

	public SchroederReverb(int sampleRate, int numberOfChannels, int SAMPLEBUFFERSIZE) {

		// Instantiate the comb filters and the allpass networks
		comb1 = new CombFilter(sampleRate, numberOfChannels, COMB1DELAYMSDEF, SAMPLEBUFFERSIZE);
		comb2 = new CombFilter(sampleRate, numberOfChannels, COMB2DELAYMSDEF, SAMPLEBUFFERSIZE);
		comb3 = new CombFilter(sampleRate, numberOfChannels, COMB3DELAYMSDEF, SAMPLEBUFFERSIZE);
		comb4 = new CombFilter(sampleRate, numberOfChannels, COMB4DELAYMSDEF, SAMPLEBUFFERSIZE);

		allpass1 = new AllpassNetwork(sampleRate, numberOfChannels, ALLPASS1DELAYMSDEF, SAMPLEBUFFERSIZE);
		allpass2 = new AllpassNetwork(sampleRate, numberOfChannels, ALLPASS2DELAYMSDEF, SAMPLEBUFFERSIZE);

		// Set initial value for sustain
		setSustainInMs(SUSTAINTIMEMSDEF);

		// Set dry/wet mix to initial value
		mix = MIXDEF;
	}

	// Set the comb filter delays
	public void setComb1Delay(double delay) {

		comb1.setDelayInMs(delay);
	}

	public double getComb1Delay() {
		return comb1.getDelayInMs();
	}

	public void setComb2Delay(double delay) {

		comb2.setDelayInMs(delay);
	}
	
	public double getComb2Delay() {
		return comb2.getDelayInMs();
	}

	public void setComb3Delay(double delay) {

		comb3.setDelayInMs(delay);
	}
	
	public double getComb3Delay() {
		return comb3.getDelayInMs();
	}

	public void setComb4Delay(double delay) {

		comb4.setDelayInMs(delay);
	}

	public double getComb4Delay() {
		return comb4.getDelayInMs();
	}

	// Set the allpass filter delays
	public void setAllpass1Delay(double delay) {

		allpass1.setDelayInMs(delay);
	}

	public double getAllpass1Delay() {
		return allpass1.getDelayInMs();
	}
	
	public void setAllpass2Delay(double delay) {
		
		allpass2.setDelayInMs(delay);
	}

	public double getAllpass2Delay() {
		return allpass2.getDelayInMs();
	}
	
	public void setSustainInMs(double sustainInMs) {

		// Set sustain in all comb filters
		comb1.setSustainTimeInMs(sustainInMs);
		comb2.setSustainTimeInMs(sustainInMs);
		comb3.setSustainTimeInMs(sustainInMs);
		comb4.setSustainTimeInMs(sustainInMs);
		
		// Allpass filter sustain is set by model
		allpass1.setSustainTimeInMs(ALLPASS1SUSTAINMSDEF);
		allpass2.setSustainTimeInMs(ALLPASS2SUSTAINMSDEF);
	}

	public double getSustainInMs() {
		return allpass1.getSustainTimeInMs();
	}
	

	// Set the mix between the dry and the wet signal
	public void setDryWetMix(double mix) {

		this.mix = mix;
	}

	public double getMix() {
		return mix;
	}
	
	// Process a buffer of samples at a time thru the reverb
	public int doReverb(short [] inBuf, int length) {

		// Allocate buffer as required. Buffer must be initialized
		// to zeros.
		if (length != -1)
			dBuffer  = new double[length];

		// Apply the combs in parallel, get the possibly new length.
		// All combs should return the same length.
		
		int newLength = 
		comb1.doFilter(inBuf, dBuffer, length);
		comb2.doFilter(inBuf, dBuffer, length);
		comb3.doFilter(inBuf, dBuffer, length);
		comb4.doFilter(inBuf, dBuffer, length);

		boolean inputExhausted = (newLength == -1);

		if (!inputExhausted) {
			// Scale the data
			for (int i=0; i < newLength; i++)
				dBuffer[i] *= 0.25;

		}	else	{
			newLength = dBuffer.length;
		}
		double [] dBuffer1 = new double[newLength];

		// Apply the allpass networks
		length = 
			allpass1.doFilter(dBuffer, dBuffer1, inputExhausted ? -1:newLength);
		
		length = 
			allpass2.doFilter(dBuffer1, dBuffer, length);

		// Apply the mix
		if (!inputExhausted) {
			// Mix the dry input samples with the processed samples
			for (int i=0; i < length; i++) {
				double s = (inBuf[i] * (1.0 - mix)) + (dBuffer[i] * mix);
				if (s > 32767.0)
					s = 32767.0;
				else if (s < -32768.0)
					s = -32768.0;

				inBuf[i] = (short) s;
			}
		
		}	else {
			
			// Only wet samples are available
			for (int i=0; i < length; i++) {
				double s = dBuffer[i] * mix;
				if (s > 32767.0)
					s = 32767.0;
				else if (s < -32768.0)
					s = -32768.0;

				inBuf[i] = (short) s;
			}
		}
		return length;
	}

	// Private class data
	private double mix;
	private CombFilter comb1;
	private CombFilter comb2;
	private CombFilter comb3;
	private CombFilter comb4;
	private AllpassNetwork allpass1;
	private AllpassNetwork allpass2;
	private double [] dBuffer = new double[1];
}




