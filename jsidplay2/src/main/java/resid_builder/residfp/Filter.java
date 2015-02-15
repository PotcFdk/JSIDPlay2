/**
 * This file is part of reSID, a MOS6581 SID emulator engine.
 * Copyright (C) 2004  Dag Lem <resid@nimrod.no>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * ---------------------------------------------------------------------------
 * Filter distortion code written by Antti S. Lankila 2007 - 2008.
 * 
 * @author Ken Händel
 *
 */
package resid_builder.residfp;

/**
 * The SID filter is modeled with a two-integrator-loop biquadratic filter, which has been confirmed by Bob Yannes to be the actual circuit used in the SID chip.
 * <P>
 * Measurements show that excellent emulation of the SID filter is achieved, except when high resonance is combined with high sustain levels. In this case the SID op-amps are performing less than
 * ideally and are causing some peculiar behavior of the SID filter. This however seems to have more effect on the overall amplitude than on the color of the sound.
 * <P>
 * The theory for the filter circuit can be found in "Microelectric Circuits" by Adel S. Sedra and Kenneth C. Smith. The circuit is modeled based on the explanation found there except that an
 * additional inverter is used in the feedback from the bandpass output, allowing the summer op-amp to operate in single-ended mode. This yields inverted filter outputs with levels independent of Q,
 * which corresponds with the results obtained from a real SID.
 * <P>
 * We have been able to model the summer and the two integrators of the circuit to form components of an IIR filter. Vhp is the output of the summer, Vbp is the output of the first integrator, and Vlp
 * is the output of the second integrator in the filter circuit.
 * <P>
 * According to Bob Yannes, the active stages of the SID filter are not really op-amps. Rather, simple NMOS inverters are used. By biasing an inverter into its region of quasi-linear operation using a
 * feedback resistor from input to output, a MOS inverter can be made to act like an op-amp for small signals centered around the switching threshold.
 * <P>
 * Qualified guesses at SID filter schematics are depicted below.
 * 
 * <pre>
 * SID filter
 * ----------
 * 
 *     -----------------------------------------------
 *    |                                               |
 *    |            ---Rq--                            |
 *    |           |       |                           |
 *    |  ------------&lt;A]-----R1---------              |
 *    | |                               |             |
 *    | |                        ---C---|      ---C---|
 *    | |                       |       |     |       |
 *    |  --R1--    ---R1--      |---Rs--|     |---Rs--|
 *    |        |  |       |     |       |     |       |
 *     ----R1--|-----[A&gt;--|--R-----[A&gt;--|--R-----[A&gt;--|
 *             |          |             |             |
 * vi -----R1--           |             |             |
 * 
 *                       vhp           vbp           vlp
 * 
 * 
 * vi  - input voltage
 * vhp - highpass output
 * vbp - bandpass output
 * vlp - lowpass output
 * [A&gt; - op-amp
 * R1  - summer resistor
 * Rq  - resistor array controlling resonance (4 resistors)
 * R   - NMOS FET voltage controlled resistor controlling cutoff frequency
 * Rs  - shunt resitor
 * C   - capacitor
 * 
 * 
 * 
 * SID integrator
 * --------------
 * 
 *                                   V+
 * 
 *                                   |
 *                                   |
 *                              -----|
 *                             |     |
 *                             | ||--
 *                              -||
 *                   ---C---     ||-&gt;
 *                  |       |        |
 *                  |---Rs-----------|---- vo
 *                  |                |
 *                  |            ||--
 * vi ----     -----|------------||
 *        |   &circ;     |            ||-&gt;
 *        |___|     |                |
 *        -----     |                |
 *          |       |                |
 *          |---R2--                 |
 *          |
 *          R1                       V-
 *          |
 *          |
 * 
 *          Vw
 * ----------------------------------------------------------------------------
 * </pre>
 * 
 * @author Ken Händel
 */
public abstract class Filter {

	/**
	 * Filter enabled.
	 */
	private boolean enabled = true;

	/**
	 * Filter cutoff frequency.
	 */
	protected int fc;

	/**
	 * Filter resonance.
	 */
	protected int res;

	/**
	 * Selects which inputs to route through filter.
	 */
	private byte filt;
	protected boolean filt1, filt2, filt3, filtE;

	/**
	 * Switch voice 3 off.
	 */
	protected boolean voice3off;

	/**
	 * Highpass, bandpass, and lowpass filter modes.
	 */
	protected boolean hp, bp, lp;

	protected float vol; /* to avoid integer-to-float conversion at output */

	protected double clockFrequency;

	protected float Vhp, Vbp, Vlp;

	/* Resonance/Distortion/Type3/Type4 helpers. */
	protected float _1_div_Q;

	protected float resonanceFactor;

	/**
	 * SID clocking - 1 cycle
	 * 
	 * @param v1 voice 1 in
	 * @param v2 voice 2 in
	 * @param v3 voice 3 in
	 * @param vE external audio in
	 * @return filtered output
	 */

	protected abstract float clock(float v1, float v2, float v3, float vE);

	protected final void zeroDenormals() {
		/* We only need this for systems that don't do -msse and -mfpmath=sse */
		if (Vbp > -1e-12f && Vbp < 1e-12f) {
			Vbp = 0;
		}
		if (Vlp > -1e-12f && Vlp < 1e-12f) {
			Vlp = 0;
		}
	}

	public abstract void setCurveAndDistortionDefaults();

	/**
	 * Enable filter.
	 * 
	 * @param enable
	 */
	public void enable(final boolean enable) {
		enabled = enable;
		if (enabled) {
			writeRES_FILT(filt);
		} else {
			filt1 = filt2 = filt3 = filtE = false;
		}
	}

	protected void setClockFrequency(final double clock) {
		clockFrequency = clock;
		updatedCenterFrequency();
	}

	public abstract float[] getCurveProperties();

	public abstract void setCurveProperties(float a, float b, float c, float d);

	public abstract float[] getDistortionProperties();

	public abstract void setDistortionProperties(float a, float b, float c);

	/**
	 * SID reset.
	 */
	protected final void reset() {
		Vhp = Vbp = Vlp = 0;
		writeFC_LO((byte) 0);
		writeFC_HI((byte) 0);
		writeMODE_VOL((byte) 0);
		writeRES_FILT((byte) 0);
	}

	// ----------------------------------------------------------------------------
	// Register functions.
	// ----------------------------------------------------------------------------

	/**
	 * Register functions.
	 * 
	 * @param fc_lo
	 */
	protected final void writeFC_LO(final byte fc_lo) {
		fc = fc & 0x7f8 | fc_lo & 0x007;
		updatedCenterFrequency();
	}

	/**
	 * Register functions.
	 * 
	 * @param fc_hi
	 */
	protected final void writeFC_HI(final byte fc_hi) {
		fc = fc_hi << 3 & 0x7f8 | fc & 0x007;
		updatedCenterFrequency();
	}

	/**
	 * Register functions.
	 * 
	 * @param res_filt
	 */
	protected final void writeRES_FILT(final byte res_filt) {
		filt = res_filt;

		res = res_filt >> 4 & 0x0f;
		updatedResonance();

		if (enabled) {
			filt1 = (filt & 1) != 0;
			filt2 = (filt & 2) != 0;
			filt3 = (filt & 4) != 0;
			filtE = (filt & 8) != 0;
		}
	}

	/**
	 * Register functions.
	 * 
	 * @param mode_vol
	 */
	protected final void writeMODE_VOL(final byte mode_vol) {
		vol = (mode_vol & 0xf) / 15.f;
		lp = (mode_vol & 0x10) != 0;
		bp = (mode_vol & 0x20) != 0;
		hp = (mode_vol & 0x40) != 0;
		voice3off = (mode_vol & 0x80) == 0;
	}

	/**
	 * Set filter cutoff frequency.
	 */
	protected abstract void updatedCenterFrequency();

	/**
	 * Set filter resonance.
	 */
	protected abstract void updatedResonance();
}
