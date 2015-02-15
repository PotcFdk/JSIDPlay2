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

import libsidplay.common.ChipModel;

public final class Voice {
	public final WaveformGenerator wave = new WaveformGenerator();

	public final EnvelopeGenerator envelope = new EnvelopeGenerator();

	/** Multiplying D/A DC offset. */
	private float voiceOffset;

	/**
	 * Amplitude modulated waveform output.
	 *
	 * The waveform DAC generates a voltage between 5 and 12 V corresponding
	 * to oscillator state 0 .. 4095.
	 *
	 * The envelope DAC generates a voltage between waveform gen output and
	 * the 5V level, corresponding to envelope state 0 .. 255.
	 *
	 * Ideal range [-2048*255, 2047*255].
	 *
	 * @param ringModulator Ring-modulator for waveform
	 * @return waveformgenerator output
	 */
	float output(final WaveformGenerator ringModulator) {
		return wave.output(ringModulator) * envelope.output() + voiceOffset;
	}

	/**
	 * Constructor.
	 */
	protected Voice() {}

	/**
	 * Set chip model.
	 * 
	 * There is some level from each voice even if the env is down and osc
	 * is stopped. You can hear this by routing a voice into filter (filter
	 * should be kept disabled for this) as the master level changes. This
	 * tunable determines this static offset and affects the volume of digis.
	 * 
	 * 6581 digis are quite loud, and 8580 digis still there but much fainter.
	 *
	 * @param model chip model to use
	 */
	public void setChipModel(final ChipModel model) {
		if (model == ChipModel.MOS6581) {
			voiceOffset = 0x800 * 0xff;
		} else {
			voiceOffset = -0x100 * 0xff;
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
