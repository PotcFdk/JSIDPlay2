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

/**
 * A 15 bit counter is used to implement the envelope rates, in effect dividing
 * the clock to the envelope counter by the currently selected rate period.
 * <P>
 * In addition, another counter is used to implement the exponential envelope decay, in effect further dividing the clock to the envelope counter. The period of this counter is set to 1, 2, 4, 8, 16,
 * 30 at the envelope counter values 255, 93, 54, 26, 14, 6, respectively.
 * 
 * @author Ken Händel
 * 
 */
public final class EnvelopeGenerator {
	/**
	 * The envelope state machine's distinct states. In addition to this,
	 * envelope has a hold mode, which freezes envelope counter to zero.
	 */
	private static enum State {
		ATTACK, DECAY_SUSTAIN, RELEASE
	}

	/**
	 * Whether hold is enabled. Only switching to ATTACK can release envelope.
	 */
	boolean hold;

	/**
	 * XOR shift register emulated via normal integer which implements delay
	 * until the next envelope operation occurs. The XOR shift register has
	 * 0x7fff different values to scan.
	 */
	private int rateCounter;

	/**
	 * Comparison value (period) of the rate counter before next event.
	 */
	private int rateCounterPeriod;

	/**
	 * During release mode, the SID arpproximates envelope decay via piecewise
	 * linear decay rate.
	 */
	private int exponentialCounter;

	/**
	 * Comparison value (period) of the exponential decay counter before next
	 * decrement.
	 */
	private int exponentialCounterPeriod;

	/** The current digital value of envelope output. */
	private byte envelopeValue;

	/** The current analog value of envelope output. */
	private float envelopeValueDac;

	/** Voice mute (kill envelope output) */
	private boolean muted;

	/** Attack register */
	private int attack;

	/** Decay register */
	private int decay;

	/** Sustain register */
	private int sustain;

	/** Release register */
	private int release;

	/** Gate bit */
	private boolean gate;

	/** Current envelope state */
	private State state;

	/**
	 * Lookup table to convert from attack, decay, or release value to rate
	 * counter period.
	 * <P>
	 * Rate counter periods are calculated from the Envelope Rates table in the Programmer's Reference Guide. The rate counter period is the number of cycles between each increment of the envelope
	 * counter. The rates have been verified by sampling ENV3.
	 * <P>
	 * The rate counter is a 16 bit register which is incremented each cycle. When the counter reaches a specific comparison value, the envelope counter is incremented (attack) or decremented
	 * (decay/release) and the counter is zeroed.
	 * <P>
	 * NB! Sampling ENV3 shows that the calculated values are not exact. It may seem like most calculated values have been rounded (.5 is rounded down) and 1 has beed added to the result. A possible
	 * explanation for this is that the SID designers have used the calculated values directly as rate counter comparison values, not considering a one cycle delay to zero the counter. This would
	 * yield an actual period of comparison value + 1.
	 * <P>
	 * The time of the first envelope count can not be exactly controlled, except possibly by resetting the chip. Because of this we cannot do cycle exact sampling and must devise another method to
	 * calculate the rate counter periods.
	 * <P>
	 * The exact rate counter periods can be determined e.g. by counting the number of cycles from envelope level 1 to envelope level 129, and dividing the number of cycles by 128. CIA1 timer A and B
	 * in linked mode can perform the cycle count. This is the method used to find the rates below.
	 * <P>
	 * To avoid the ADSR delay bug, sampling of ENV3 should be done using sustain = release = 0. This ensures that the attack state will not lower the current rate counter period.
	 * <P>
	 * The ENV3 sampling code below yields a maximum timing error of 14 cycles.
	 * 
	 * <pre>
	 *      lda #$01
	 *  l1: cmp $d41c
	 *      bne l1
	 *      ...
	 *      lda #$ff
	 *  l2: cmp $d41c
	 *      bne l2
	 * </pre>
	 * 
	 * This yields a maximum error for the calculated rate period of 14/128 cycles. The described method is thus sufficient for exact calculation of the rate periods.
	 */
	private static final int[] ENVELOPE_PERIOD = {
		9, //   2ms*1.0MHz/256 =     7.81
		32, //   8ms*1.0MHz/256 =    31.25
		63, //  16ms*1.0MHz/256 =    62.50
		95, //  24ms*1.0MHz/256 =    93.75
		149, //  38ms*1.0MHz/256 =   148.44
		220, //  56ms*1.0MHz/256 =   218.75
		267, //  68ms*1.0MHz/256 =   265.63
		313, //  80ms*1.0MHz/256 =   312.50
		392, // 100ms*1.0MHz/256 =   390.63
		977, // 250ms*1.0MHz/256 =   976.56
		1954, // 500ms*1.0MHz/256 =  1953.13
		3126, // 800ms*1.0MHz/256 =  3125.00
		3907, //   1 s*1.0MHz/256 =  3906.25
		11720, //   3 s*1.0MHz/256 = 11718.75
		19532, //   5 s*1.0MHz/256 = 19531.25
		31251 //   8 s*1.0MHz/256 = 31250.00
	};

	/**
	 * The 16 selectable sustain levels.
	 * <P>
	 * For decay and release, the clock to the envelope counter is sequentially divided by 1, 2, 4, 8, 16, 30, 1 to create a piece-wise linear approximation of an exponential. The exponential counter
	 * period is loaded at the envelope counter values 255, 93, 54, 26, 14, 6, 0. The period can be different for the same envelope counter value, depending on whether the envelope has been rising
	 * (attack -> release) or sinking (decay/release).
	 * <P>
	 * Since it is not possible to reset the rate counter (the test bit has no influence on the envelope generator whatsoever) a method must be devised to do cycle exact sampling of ENV3 to do the
	 * investigation. This is possible with knowledge of the rate period for A=0, found above.
	 * <P>
	 * The CPU can be synchronized with ENV3 by first synchronizing with the rate counter by setting A=0 and wait in a carefully timed loop for the envelope counter _not_ to change for 9 cycles. We
	 * can then wait for a specific value of ENV3 with another timed loop to fully synchronize with ENV3.
	 * <P>
	 * At the first period when an exponential counter period larger than one is used (decay or relase), one extra cycle is spent before the envelope is decremented. The envelope output is then
	 * delayed one cycle until the state is changed to attack. Now one cycle less will be spent before the envelope is incremented, and the situation is normalized.
	 * <P>
	 * The delay is probably caused by the comparison with the exponential counter, and does not seem to affect the rate counter. This has been verified by timing 256 consecutive complete envelopes
	 * with A = D = R = 1, S = 0, using CIA1 timer A and B in linked mode. If the rate counter is not affected the period of each complete envelope is
	 * <P>
	 * (255 + 162*1 + 39*2 + 28*4 + 12*8 + 8*16 + 6*30)*32 = 756*32 = 32352
	 * <P>
	 * which corresponds exactly to the timed value divided by the number of complete envelopes.
	 * <P>
	 * <P>
	 * From the sustain levels it follows that both the low and high 4 bits of the envelope counter are compared to the 4-bit sustain value. This has been verified by sampling ENV3.
	 */

	/**
	 * Emulated nonlinearity of the envelope DAC.
	 * 
	 * @See SID.kinked_dac
	 */
	private final float[] dac = new float[256];

	/**
	 * SID clocking - 1 cycle.
	 */
	protected void clock() {
		if (++rateCounter != rateCounterPeriod) {
			return;
		}
		rateCounter = 0;

		// The first envelope step in the attack state also resets the
		// exponential counter. This has been verified by sampling ENV3.
		//
		if (state == State.ATTACK || ++exponentialCounter == exponentialCounterPeriod) {
			exponentialCounter = 0;

			if (hold) {
				return;
			}

			if (state == State.ATTACK) {
				// The envelope counter can flip from 0xff to 0x00 by changing
				// state to release, then to attack. The envelope counter is
				// then frozen at zero; to unlock this situation the state must
				// be changed to release, then to attack. This has been verified
				// by sampling ENV3.
				if (++ envelopeValue == (byte) 0xff) {
					state = State.DECAY_SUSTAIN;
					/* no ADSR delay bug possible, because rateCounter = 0 */
					rateCounterPeriod = ENVELOPE_PERIOD[decay];
				}
			} else if (state == State.DECAY_SUSTAIN) {
				if (envelopeValue != (byte) (sustain << 4 | sustain)) {
					--envelopeValue;
				}
			} else if (state == State.RELEASE) {
				// The envelope counter can flip from 0x00 to 0xff by changing
				// state to attack, then to release. The envelope counter will
				// then continue counting down in the release state.
				// This has been verified by sampling ENV3.
				--envelopeValue;
			}

			// Check for change of exponential counter period.
			switch (envelopeValue) {
			case (byte) 0xff:
				exponentialCounterPeriod = 1;
			break;
			case 0x5d:
				exponentialCounterPeriod = 2;
				break;
			case 0x36:
				exponentialCounterPeriod = 4;
				break;
			case 0x1a:
				exponentialCounterPeriod = 8;
				break;
			case 0x0e:
				exponentialCounterPeriod = 16;
				break;
			case 0x06:
				exponentialCounterPeriod = 30;
				break;
			case 0x00:
				exponentialCounterPeriod = 1;
				hold = true;
				break;
			}

			envelopeValueDac = muted ? 0 : dac[envelopeValue & 0xff];
		}
	}

	/**
	 * Set nonlinearity parameter for imperfect analog DAC emulation.
	 * 1.0 means perfect 8580-like linearity, values between 0.95 - 0.97
	 * are probably realistic 6581 nonlinearity values.
	 * 
	 * @param nonLinearity
	 */
	protected void setNonLinearity(final float nonLinearity) {
		for (int i = 0; i < 256; i++) {
			dac[i] = SID.kinkedDac(i, nonLinearity, 8);
		}
	}

	/**
	 * Constructor.
	 */
	protected EnvelopeGenerator() {}

	/**
	 * SID reset.
	 */
	protected void reset() {
		envelopeValue = 0;
		envelopeValueDac = 0;

		attack = 0;
		decay = 0;
		sustain = 0;
		release = 0;

		gate = false;

		rateCounter = 0;
		exponentialCounter = 0;
		exponentialCounterPeriod = 1;

		state = State.RELEASE;
		hold = false;
		rateCounterPeriod = ENVELOPE_PERIOD[release];
	}

	/**
	 * Mute this voice. (Triggered at next envelope event.)
	 * 
	 * @param enable
	 */
	protected void mute(final boolean enable) {
		muted = enable;
	}

	// ----------------------------------------------------------------------------
	// Register functions.
	// ----------------------------------------------------------------------------

	/**
	 * @param control
	 *            control register
	 */
	protected void writeCONTROL_REG(final byte control) {
		final boolean gate_next = (control & 0x01) != 0;

		// The rate counter is never reset, thus there will be a delay before the
		// envelope counter starts counting up (attack) or down (release).

		// Gate bit on: Start attack, decay, sustain.
		if (!gate && gate_next) {
			state = State.ATTACK;
			cpuUpdateRatePeriod(ENVELOPE_PERIOD[attack]);
			hold = false;
		}
		// Gate bit off: Start release.
		else if (gate && !gate_next) {
			state = State.RELEASE;
			cpuUpdateRatePeriod(ENVELOPE_PERIOD[release]);
		}

		gate = gate_next;
	}

	/**
	 * @param attack_decay
	 *            attack/decay value
	 */
	protected void writeATTACK_DECAY(final byte attack_decay) {
		attack = attack_decay >> 4 & 0x0f;
		decay = attack_decay & 0x0f;
		if (state == State.ATTACK) {
			cpuUpdateRatePeriod(ENVELOPE_PERIOD[attack]);
		} else if (state == State.DECAY_SUSTAIN) {
			cpuUpdateRatePeriod(ENVELOPE_PERIOD[decay]);
		}
	}

	/**
	 * @param sustain_release
	 *            sustain/release value
	 */
	protected void writeSUSTAIN_RELEASE(final byte sustain_release) {
		sustain = sustain_release >> 4 & 0x0f;
				release = sustain_release & 0x0f;
				if (state == State.RELEASE) {
					cpuUpdateRatePeriod(ENVELOPE_PERIOD[release]);
				}
	}

	/**
	 * Return the envelope current value.
	 *
	 * @return envelope counter
	 */
	public byte readENV() {
		return envelopeValue;
	}

	/**
	 * Return the analog value of envelope.
	 * 
	 * @return envelope analog output
	 */
	public float output() {
		return envelopeValueDac;
	}

	/**
	 * When CPU updates rate period, check for ADSR delay bug before
	 * allowing the update to proceed.
	 * 
	 * @param newRateCounterPeriod
	 */
	private void cpuUpdateRatePeriod(final int newRateCounterPeriod) {
		/* Handle edge case: cpu writing the same value that is already set
		 * in the register. This can't cause a ADSR delay bug. */
		if (rateCounterPeriod == newRateCounterPeriod) {
			return;
		}
		rateCounterPeriod = newRateCounterPeriod;

		/* The ADSR counter is XOR shift register with 0x7fff unique values.
		 * If the rate_period is adjusted to a value already seen in this cycle,
		 * the register will wrap around. This is known as the ADSR delay bug.
		 *
		 * To simplify the hot path calculation, we simulate this through observing
		 * that we add the 0x7fff cycle delay by changing the rate_counter variable
		 * directly. This takes care of the 99 % common case. However, playroutine
		 * could make multiple consecutive rate_period adjustments, in which case we
		 * need to cancel the previous adjustment. */

		/* if the new period exceeds 0x7fff, we need to wrap */
		if (rateCounterPeriod - rateCounter > 0x7fff) {
			rateCounter += 0x7fff;
		}

		/* simulate 0x7fff wrap-around, if the period is less than the current value */
		if (rateCounterPeriod <= rateCounter) {
			rateCounter -= 0x7fff;
		}

		/* What about adjustment that sets period to the exact same value as the XOR
		 * register? This would warrant testing, complicated though as it is. We know,
		 * however, that the SID is clocked at PHI1 and the write from CPU arrives at
		 * PHI2. It is probable that the SID has already determined whether XOR value
		 * matches at that clock, and the CPU adjustment can only take effect the next
		 * cycle. This consideration is reflected by the > rather than >= and
		 * <= rather than < in the two comparisons above.
		 */
	}
}
