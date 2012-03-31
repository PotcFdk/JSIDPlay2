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
package resid_builder.resid;

import resid_builder.resid.ISIDDefs.ChipModel;

public final class Voice {
	public final WaveformGenerator wave = new WaveformGenerator();

	public final EnvelopeGenerator envelope = new EnvelopeGenerator();

	/** Multiplying D/A DC offset. */
	private int voiceOffset;

	/**
	 * Amplitude modulated waveform output.
	 * Ideal range [-2048*255, 2047*255].
	 *
	 * @param ringModulator Ring-modulator for waveform
	 * @return waveformgenerator output
	 */
	protected int output(final WaveformGenerator ringModulator) {
		return wave.output(ringModulator) * envelope.output() + voiceOffset;
	}

	/**
	 * Constructor.
	 */
	protected Voice() {}

	/**
	 * Set chip model.
	 * 
	 * @param model chip model to use
	 */
	public void setChipModel(final ChipModel model) {
		if (model == ChipModel.MOS6581) {
			/* there is some level from each voice even if the env is down and osc
			 * is stopped. You can hear this by routing a voice into filter (filter
			 * should be kept disabled for this) as the master level changes. This
			 * tunable affects the volume of digis. */
			voiceOffset = 0x800 * 0xff;
			/* In 8580 the waveforms seem well centered, but on the 6581 there is some
			 * offset change as envelope grows, indicating that the waveforms are not
			 * perfectly centered. The likely cause for this is the follows:
			 *
			 * The waveform DAC generates a voltage between 5 and 12 V corresponding
			 * to oscillator state 0 .. 4095.
			 *
			 * The envelope DAC generates a voltage between waveform gen output and
			 * the 5V level.
			 *
			 * The outputs are amplified against the 12V voltage and sent to the
			 * mixer.
			 *
			 * The SID virtual ground is around 6.5 V. */
		} else {
			voiceOffset = 0;
		}
	}

	// ----------------------------------------------------------------------------
	// Register functions.
	// ----------------------------------------------------------------------------

	/**
	 * Register functions.
	 * 
	 * @param ring_modulator Ring modulator for waveform
	 * @param control Control register value.
	 */
	public void writeCONTROL_REG(final WaveformGenerator ring_modulator, final byte control) {
		wave.writeCONTROL_REG(ring_modulator, control);
		envelope.writeCONTROL_REG(control);
	}

	/**
	 * SID reset.
	 */
	public void reset() {
		wave.reset();
		envelope.reset();
	}

	public boolean muted;

	/**
	 * Voice mute.
	 * 
	 * @param enable Is voice enabled?
	 */
	public void mute(final boolean enable) {
		muted = enable;
		envelope.mute(enable);
	}

}
