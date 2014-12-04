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
package residfp_builder.resid;

import libsidplay.common.ChipModel;

/**
 * A 24 bit accumulator is the basis for waveform generation. FREQ is added to the lower 16 bits of the accumulator each cycle. The accumulator is set to zero when TEST is set, and starts counting
 * when TEST is cleared. The noise waveform is taken from intermediate bits of a 23 bit shift register. This register is clocked by bit 19 of the accumulator.
 * 
 * @author Ken Händel
 * 
 */
public final class WaveformGenerator {
	private float[][] wftable;
	private byte[][] wfdigital;
	private float[] dac;

	protected void setWftable(final float[][] newWftable, final float[] newDac, final byte[][] newWfdigital) {
		wftable = newWftable;
		wfdigital = newWfdigital;
		dac = newDac;
	}

	/** Current and previous accumulator value. */
	private int accumulator, accumulator_prev;
	
	/** Value in noise XOR register. */
	private int noiseShiftRegister;
	
	/** Time until Noise XOR register is overwritten. */
	private int noiseShiftRegisterTtl;

	/** Cached digital value of OSC3. */
	private byte previous;

	/** Cached analog value of OSC3 */
	private float previous_dac;
	
	/**
	 * Fout = (Fn*Fclk/16777216)Hz
	 */
	private int freq;

	/**
	 * PWout = (PWn/40.95)%
	 */
	private int pw;

	/**
	 * The control register right-shifted 4 bits; used for output function table lookup.
	 */
	private int waveform;

	/**
	 * The control register bits. Gate is handled by EnvelopeGenerator.
	 */
	private boolean test, ring, sync;

	/**
	 * SID clocking - 1 cycle.
	 */
	protected void clock() {
		/* no digital operation if test bit is set. Only emulate analog fade. */
		if (test) {
			if (noiseShiftRegisterTtl != 0) {
				if (--noiseShiftRegisterTtl == 0) {
					noiseShiftRegister |= 0x7ffffc;
					clockNoise(false);
				}
			}
			return;
		}

		accumulator_prev = accumulator;

		// Calculate new accumulator value;
		accumulator = (accumulator + freq) & 0xffffff;

		// Shift noise register once for each time accumulator bit 19 is set high.
		if (((~accumulator_prev) & accumulator & 0x080000) != 0) {
			clockNoise(true);
		} // Shift noise register once for each time accumulator bit 19 is set
	}

	/**
	 * Synchronize oscillators. This must be done after all the oscillators have been clock()'ed since the oscillators operate in parallel. Note that the oscillators must be clocked exactly on the
	 * cycle when the MSB is set high for hard sync to operate correctly. See SID.clock().
	 *
	 * @param syncDest The oscillator I am syncing
	 * @param syncSource The oscillator syncing me.
	 */
	protected final void synchronize(final WaveformGenerator syncDest, final WaveformGenerator syncSource) {
		// A special case occurs when a sync source is synced itself on the same
		// cycle as when its MSB is set high. In this case the destination will
		// not be synced. This has been verified by sampling OSC3.
		if (syncDest.sync && ((~accumulator_prev) & accumulator & 0x800000) != 0
				&& !(sync && ((~syncSource.accumulator_prev) & syncSource.accumulator & 0x800000) != 0)) {
			syncDest.accumulator = 0;
		}
	}

	/**
	 * 12-bit waveform output. Select one of 16 possible combinations of waveforms.
	 * 
	 * @param ring_modulator The oscillator ring-modulating me.
	 * @return output from waveformgenerator
	 */
	public float output(final WaveformGenerator ring_modulator) {
		if (waveform == 0 || waveform > 7) {
			return previous_dac;
		}
		/* waveforms 1 .. 7 left */

		/* Phase for all waveforms */
		int phase = accumulator >> 12;
		/* pulse on/off generates 4 more variants after the main pulse types */
		final int variant = waveform >= 4 && (test || phase >= pw) ? 3 : -1;

		/*
		 * triangle waveform XOR circuit. Since the table already makes a triangle wave internally, we only need to account for the sync source here. Flipping the top bit suffices to reproduce the
		 * original SID ringmod
		 */
		phase ^= ring && 0 != (ring_modulator.accumulator & 0x800000) ? 0x800 : 0x00;

		return wftable[waveform + variant][phase];
	}

	/**
	 * Constructor.
	 */
	protected WaveformGenerator() {}

	/**
	 * Register functions.
	 * 
	 * @param freq_lo low 8 bits of frequency
	 */
	protected void writeFREQ_LO(final byte freq_lo) {
		freq = freq & 0xff00 | freq_lo & 0xff;
	}

	/**
	 * Register functions.
	 * 
	 * @param freq_hi high 8 bits of frequency
	 */
	protected void writeFREQ_HI(final byte freq_hi) {
		freq = freq_hi << 8 & 0xff00 | freq & 0xff;
	}

	/**
	 * Register functions.
	 * 
	 * The original form was (acc >> 12) >= pw, where truth value is not affected by the contents of the low 12 bits. Therefore the lowest bits must be zero in the new formulation acc >= (pw << 12).
	 * 
	 * @param pw_lo low 8 bits of pulse width
	 */
	protected void writePW_LO(final byte pw_lo) {
		pw = pw & 0xf00 | pw_lo & 0x0ff;
	}

	/**
	 * Register functions.
	 * 
	 * @param pw_hi high 8 bits of pulse width
	 */
	protected void writePW_HI(final byte pw_hi) {
		pw = pw_hi << 8 & 0xf00 | pw & 0x0ff;
	}

	/**
	 * Register functions.
	 * 
	 * @param ring_modulator ring-modulator modulating me.
	 * @param control control register value
	 */
	protected void writeCONTROL_REG(final WaveformGenerator ring_modulator, final byte control) {
		/*
		 * when selecting the 0 waveform, the previous output is held for a time in the DAC MOSFET gates. clock_noise deals with waveform values >= 8.
		 */
		final int waveform_next = control >> 4 & 0x0f;
		if (waveform_next == 0 && waveform >= 1 && waveform <= 7) {
			/* NB: this should always be the "6581" version (not delayed). */
			previous = readOSC6581(ring_modulator);
			previous_dac = output(ring_modulator);
		}

		waveform = waveform_next;
		ring = (control & 0x04) != 0 && (waveform & 0x3) == 1;
		sync = (control & 0x02) != 0;
		final boolean test_next = (control & 0x08) != 0;

		// Test bit rising? Invert bit 19 and write it to bit 1.
		if (test_next && !test) {
			accumulator = 0;
			accumulator_prev = 0;
			final int bit19 = noiseShiftRegister >> 18 & 2;
		noiseShiftRegister = noiseShiftRegister & 0x7ffffd | bit19 ^ 2;
		noiseShiftRegisterTtl = 200000; /* 200 ms, probably too generous? */
		} else {
			if (!test_next) {
				/* clock noise if test bit is falling */
				clockNoise(test);
			}
		}

		test = test_next;
	}

	/**
	 * Read OSC3 value (6581, not latched/delayed version)
	 * 
	 * @param ring_modulator The ring modulating partner of this waveform
	 * @return OSC3 value
	 */
	protected byte readOSC6581(final WaveformGenerator ring_modulator) {
		return readOSC(ring_modulator.accumulator, accumulator);
	}

	/**
	 * Read OSC3 value (8580, 1-clock latched version).
	 * Waveforms 0 and 8 and above are not appropriately delayed by 1 clock.
	 * It should not be noticeable for 0 and > 8, but noise is not correctly
	 * delayed.
	 * 
	 * @param ring_modulator The ring modulating partner of this waveform
	 * @return OSC3 value
	 */
	protected byte readOSC8580(final WaveformGenerator ring_modulator) {
		return readOSC(ring_modulator.accumulator_prev, accumulator_prev);
	}
	
	public byte readOSC(ChipModel model) {
		if (model == ChipModel.MOS6581) {
			return readOSC6581(this);
		} else {
			return readOSC8580(this);
		}
	}

	/**
	 * Calculate OSC3 bitstate from the analog values.
	 * 
	 * @param ringAccumulator
	 * @param accumulator
	 * @return
	 */
	private byte readOSC(int ringAccumulator, int myAccumulator) {
		if (waveform == 0 || waveform >= 8) {
			return previous;
		}
		
		/* Phase for all waveforms */
		int phase = myAccumulator >> 12;
		/* pulse on/off generates 4 more variants after the main pulse types */
		final int variant = waveform >= 4 && (test || phase >= pw) ? 3 : -1;

		/*
		 * triangle waveform XOR circuit. Since the table already makes a triangle wave internally, we only need to account for the sync source here. Flipping the top bit suffices to reproduce the
		 * original SID ringmod
		 */
		phase ^= ring && 0 != (ringAccumulator & 0x800000) ? 0x800 : 0x00;

		return wfdigital[waveform + variant][phase];
	}

	private void clockNoise(final boolean clock) {
		if (clock) {
			final int bit0 = (noiseShiftRegister >> 22 ^ noiseShiftRegister >> 17) & 0x1;
			noiseShiftRegister = noiseShiftRegister << 1 | bit0;
		}

		// clear output bits of shift register if noise and other waveforms
		// are selected simultaneously
		if (waveform > 8) {
			noiseShiftRegister &= 0x7fffff ^ 1 << 22 ^ 1 << 20 ^ 1 << 16 ^ 1 << 13 ^ 1 << 11 ^ 1 << 7 ^ 1 << 4 ^ 1 << 2;
		}

		if (waveform >= 8) {
			/* 12 bits -- we calculate the value and shift 4 away */
			previous = calculateCurrentNoiseValue();
			previous_dac = getZeroLevel();
			for (int i = 0; i < 8; i++) {
				if ((previous & 1 << i) != 0) {
					previous_dac += dac[i + 4];
				}
			}
		}
	}

	/**
	 * Noise: The noise output is taken from intermediate bits of a 23-bit shift register which is clocked by bit 19 of the accumulator. NB! The output is actually delayed 2 cycles after bit 19 is set
	 * high. This is not modeled.
	 * <P>
	 * Operation: Calculate EOR result, shift register, set bit 0 = result.
	 * 
	 * <pre>
	 *                         ------------------------&gt;--------------------
	 *                         |                                            |
	 *                    ----EOR----                                       |
	 *                    |         |                                       |
	 *                    2 2 2 1 1 1 1 1 1 1 1 1 1                         |
	 *  Register bits:    2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 &lt;---
	 *                    |   |       |     |   |       |     |   |
	 *  OSC3 bits  :      7   6       5     4   3       2     1   0
	 * </pre>
	 * 
	 * Since waveform output is 12 bits the output is left-shifted 4 times.
	 */
	private byte calculateCurrentNoiseValue() {
		return (byte) ((noiseShiftRegister & 0x400000) >> 15
		| (noiseShiftRegister & 0x100000) >> 14
		| (noiseShiftRegister & 0x010000) >> 11
		| (noiseShiftRegister & 0x002000) >> 9
		| (noiseShiftRegister & 0x000800) >> 8
		| (noiseShiftRegister & 0x000080) >> 5
		| (noiseShiftRegister & 0x000010) >> 3
		| (noiseShiftRegister & 0x000004) >> 2);
	}

	/**
	 * SID reset.
	 */
	protected void reset() {
		accumulator_prev = accumulator = 0;
		previous = 0;
		previous_dac = 0;
		noiseShiftRegister = 0x7ffffc;
		freq = 0;
		pw = 0;
		test = false;
		waveform = 0;
		writeCONTROL_REG(this, (byte) 0);
	}

	/**
	 * Return the waveform offset. The zero level is ideally 0x800.
	 * 
	 * @return
	 */
	public float getZeroLevel() {
		return wftable[0][0];
	}
}
