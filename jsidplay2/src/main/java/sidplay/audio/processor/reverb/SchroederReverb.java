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

	protected double mix;
	protected CombFilter comb1;
	protected CombFilter comb2;
	protected CombFilter comb3;
	protected CombFilter comb4;
	protected AllpassNetwork allpass1;
	protected AllpassNetwork allpass2;

	private double [] dBuffer = new double[1];

	public SchroederReverb(int sampleRate, int numberOfChannels, int sampleBufferSize) {

		// Instantiate the comb filters and the allpass networks
		comb1 = new CombFilter(sampleRate, numberOfChannels, COMB1DELAYMSDEF, sampleBufferSize);
		comb2 = new CombFilter(sampleRate, numberOfChannels, COMB2DELAYMSDEF, sampleBufferSize);
		comb3 = new CombFilter(sampleRate, numberOfChannels, COMB3DELAYMSDEF, sampleBufferSize);
		comb4 = new CombFilter(sampleRate, numberOfChannels, COMB4DELAYMSDEF, sampleBufferSize);

		allpass1 = new AllpassNetwork(sampleRate, numberOfChannels, ALLPASS1DELAYMSDEF, sampleBufferSize);
		allpass2 = new AllpassNetwork(sampleRate, numberOfChannels, ALLPASS2DELAYMSDEF, sampleBufferSize);

		// Set initial value for sustain
		comb1.setSustainTimeInMs(SUSTAINTIMEMSDEF);
		comb2.setSustainTimeInMs(SUSTAINTIMEMSDEF);
		comb3.setSustainTimeInMs(SUSTAINTIMEMSDEF);
		comb4.setSustainTimeInMs(SUSTAINTIMEMSDEF);
		allpass1.setSustainTimeInMs(ALLPASS1SUSTAINMSDEF);
		allpass2.setSustainTimeInMs(ALLPASS2SUSTAINMSDEF);


		// Set dry/wet mix to initial value
		mix = MIXDEF;
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

}
