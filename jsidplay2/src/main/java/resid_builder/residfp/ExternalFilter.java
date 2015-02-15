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
 * 
 * @author Ken Händel
 *
 */
package resid_builder.residfp;

/**
 * 
 * The audio output stage in a Commodore 64 consists of two STC networks, a
 * low-pass filter with 3-dB frequency 16kHz followed by a high-pass filter with
 * 3-dB frequency 16Hz (the latter provided an audio equipment input impedance
 * of 1kOhm).
 * <P>
 * The STC networks are connected with a BJT supposedly meant to act as a unity gain buffer, which is not really how it works. A more elaborate model would include the BJT, however DC circuit analysis
 * yields BJT base-emitter and emitter-base impedances sufficiently low to produce additional low-pass and high-pass 3dB-frequencies in the order of hundreds of kHz. This calls for a sampling
 * frequency of several MHz, which is far too high for practical use.
 * 
 * @author Ken Händel
 * 
 */
final class ExternalFilter {
	private static final double LOWPASS_FREQUENCY = 15915.6;

	/**
	 * lowpass
	 */
	private float Vlp;

	/**
	 * highpass
	 */
	private float Vhp;

	/**
	 * Cutoff frequencies.
	 */
	private float w0lp;

	/**
	 * Cutoff frequencies.
	 */
	private float w0hp;

	/**
	 * SID clocking - 1 cycle.
	 * 
	 * @param Vi
	 */
	protected final float clock(final float Vi) {
		final float out = Vlp - Vhp;
		Vhp += w0hp * out;
		Vlp += w0lp * (Vi - Vlp);
		return out;
	}

	protected final void zeroDenormals() {
		if (Vhp > -1e-12f && Vhp < 1e-12f) {
			Vhp = 0;
		}
		if (Vlp > -1e-12f && Vlp < 1e-12f) {
			Vlp = 0;
		}
	}

	/**
	 * Constructor.
	 */
	protected ExternalFilter() {
		reset();
	}

	/**
	 * Setup of the external filter sampling parameters.
	 * 
	 * @param clock
	 */
	protected void setClockFrequency(final double frequency) {
		// Low-pass: R = 10kOhm, C = 1000pF; w0l = 1/RC = 1/(1e4*1e-9) = 100000
		// High-pass: R = 1kOhm, C = 10uF; w0h = 1/RC = 1/(1e3*1e-5) = 100
		w0hp = (float) (100 / frequency);
		w0lp = (float) (LOWPASS_FREQUENCY * 2.f * Math.PI / frequency);
	}

	/**
	 * SID reset.
	 */
	protected void reset() {
		// State of filter.
		Vlp = 0;
		Vhp = 0;
	}
}
