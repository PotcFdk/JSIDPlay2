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

public final class SID {
	/** SID voices */
	public final Voice[] voice = new Voice[] { new Voice(), new Voice(), new Voice() };

	/** Currently active filter */
	private Filter filter;

	/** Filter used, if model is set to 6581 */
	private final Filter6581 filter6581 = new Filter6581();

	/** Filter used, if model is set to 8580 */
	private final Filter8580 filter8580 = new Filter8580();

	/** Last written value */
	private byte busValue;

	/** Time to live for the last written value */
	private int busValueTtl;

	/** Interval between samples. (Not a whole integer.) */
	private float cyclesPerSample;

	/** The subclock [0, 1[ for precise subphase sampling. */
	private float sampleOffset;

	/** 6581 nonlinearity term used for all DACs */
	private float nonLinearity6581;

	/**
	 * Set DAC nonlinearity for 6581 emulation.
	 * 
	 * @param nonLinearity
	 * 
	 * @See SID.kinkedDac
	 */
	public void set6581VoiceNonlinearity(final float nonLinearity) {
		if (nonLinearity == nonLinearity6581) {
			return;
		}
		nonLinearity6581 = nonLinearity;
		if (model == ChipModel.MOS6581) {
			setChipModel(model);
		}
	}

	/**
	 * Estimate DAC nonlinearity. The SID contains R-2R ladder, but the second resistor
	 * is not exactly double the first. The parameter nonLinearity models the deviation
	 * from the resistor lengths. There appears to be about 4 % error on the 6581,
	 * resulting in major kinks on the DAC. The value that the DAC yields tends to be
	 * larger than expected. The output of this method is normalized such that DAC
	 * errors occur both above and below the ideal value equally.
	 * 
	 * @param input digital value to convert to analog
	 * @param nonLinearity nonlinearity parameter, 1.0 for perfect linearity.
	 * @param maxBit highest bit that may be set in input.
	 * @return the analog value as modeled from the R-2R network.
	 */
	public static float kinkedDac(final int input, final float nonLinearity, final int maxBit) {
		float value = 0f;
		int currentBit = 1;
		float weight = 1f;
		final float dir = 2f * nonLinearity;
		for (int i = 0; i < maxBit; i++) {
			if ((input & currentBit) != 0) {
				value += weight;
			}
			currentBit <<= 1;
			weight *= dir;
		}

		return value / (weight / nonLinearity / nonLinearity) * (1 << maxBit);
	}

	/**
	 * Constructor.
	 *
	 * @param count chip number
	 */
	public SID() {
		busValue = 0;
		busValueTtl = 0;

		set6581VoiceNonlinearity(0.96f);
		setChipModel(ChipModel.MOS8580);
		reset();
	}

	/**
	 * Currently active chip model.
	 */
	private ChipModel model;

	/**
	 * Set chip model.
	 * 
	 * @param model chip model to use
	 */
	public void setChipModel(final ChipModel model) {
		this.model = model;

		final float nonLinearity;
		if (model == ChipModel.MOS6581) {
			filter6581.setNonLinearity(nonLinearity6581);
			filter = filter6581;
			nonLinearity = nonLinearity6581;
		} else if (model == ChipModel.MOS8580) {
			filter = filter8580;
			nonLinearity = 1f;
		} else {
			throw new RuntimeException("Don't know how to handle chip type " + model);
		}

		/* calculate waveform-related tables, feed them to the generator */
		final Object[] tables = WaveformCalculator.rebuildWftable(model);

		/* update voice offsets */
		for (int i = 0; i < 3; i++) {
			voice[i].setChipModel(model);
			voice[i].envelope.setNonLinearity(nonLinearity);
			voice[i].wave.setWftable((int[][]) tables[0], (int[]) tables[1], (byte[][]) tables[2]);
		}
	}

	public ChipModel getChipModel() {
		return model;
	}

	/**
	 * SID reset.
	 */
	public void reset() {
		for (int i = 0; i < 3; i++) {
			voice[i].reset();
		}
		filter6581.reset();
		filter8580.reset();

		busValue = 0;
		busValueTtl = 0;
	}

	/**
	 * Read registers.
	 * <P>
	 * Reading a write only register returns the last byte written to any SID register. The individual bits in this value start to fade down towards zero after a few cycles. All bits reach zero within
	 * approximately $2000 - $4000 cycles. It has been claimed that this fading happens in an orderly fashion, however sampling of write only registers reveals that this is not the case. NB! This is
	 * not correctly modeled. The actual use of write only registers has largely been made in the belief that all SID registers are readable. To support this belief the read would have to be done
	 * immediately after a write to the same register (remember that an intermediate write to another register would yield that value instead). With this in mind we return the last value written to
	 * any SID register for $2000 cycles without modeling the bit fading.
	 * 
	 * @param offset SID register to read
	 * @return value read from chip
	 */
	public byte read(final int /* reg8 */offset) {
		switch (offset) {
		case 0x19:
		case 0x1a:
			return (byte) 0xff;
			
		case 0x1b:
			return model == ChipModel.MOS6581 ?
					voice[2].wave.readOSC6581(voice[0].wave) :
						voice[2].wave.readOSC8580(voice[0].wave);
		case 0x1c:
			return voice[2].envelope.readENV();
		default:
			return busValue;
		}
	}

	/**
	 * Write registers.
	 * 
	 * @param offset chip register to write
	 * @param value value to write
	 */
	public void write(final int offset, final byte value) {
		busValue = value;
		busValueTtl = 34000;

		switch (offset) {
		case 0x00:
			voice[0].wave.writeFREQ_LO(value);
			break;
		case 0x01:
			voice[0].wave.writeFREQ_HI(value);
			break;
		case 0x02:
			voice[0].wave.writePW_LO(value);
			break;
		case 0x03:
			voice[0].wave.writePW_HI(value);
			break;
		case 0x04:
			voice[0].writeCONTROL_REG(voice[1].wave, value);
			break;
		case 0x05:
			voice[0].envelope.writeATTACK_DECAY(value);
			break;
		case 0x06:
			voice[0].envelope.writeSUSTAIN_RELEASE(value);
			break;
		case 0x07:
			voice[1].wave.writeFREQ_LO(value);
			break;
		case 0x08:
			voice[1].wave.writeFREQ_HI(value);
			break;
		case 0x09:
			voice[1].wave.writePW_LO(value);
			break;
		case 0x0a:
			voice[1].wave.writePW_HI(value);
			break;
		case 0x0b:
			voice[1].writeCONTROL_REG(voice[2].wave, value);
			break;
		case 0x0c:
			voice[1].envelope.writeATTACK_DECAY(value);
			break;
		case 0x0d:
			voice[1].envelope.writeSUSTAIN_RELEASE(value);
			break;
		case 0x0e:
			voice[2].wave.writeFREQ_LO(value);
			break;
		case 0x0f:
			voice[2].wave.writeFREQ_HI(value);
			break;
		case 0x10:
			voice[2].wave.writePW_LO(value);
			break;
		case 0x11:
			voice[2].wave.writePW_HI(value);
			break;
		case 0x12:
			voice[2].writeCONTROL_REG(voice[0].wave, value);
			break;
		case 0x13:
			voice[2].envelope.writeATTACK_DECAY(value);
			break;
		case 0x14:
			voice[2].envelope.writeSUSTAIN_RELEASE(value);
			break;
		case 0x15:
			filter6581.writeFC_LO(value);
			filter8580.writeFC_LO(value);
			break;
		case 0x16:
			filter6581.writeFC_HI(value);
			filter8580.writeFC_HI(value);
			break;
		case 0x17:
			filter6581.writeRES_FILT(value);
			filter8580.writeRES_FILT(value);
			break;
		case 0x18:
			filter6581.writeMODE_VOL(value);
			filter8580.writeMODE_VOL(value);
			break;
		default:
			break;
		}
	}

	/**
	 * SID voice muting.
	 * 
	 * @param channel channe to modify
	 * @param enable is muted?
	 */
	public void mute(final int channel, final boolean enable) {
		voice[channel].mute(enable);
	}

	/**
	 * Setting of SID sampling parameters.
	 * <P>
	 * Use a clock freqency of 985248Hz for PAL C64, 1022730Hz for NTSC C64. The default end of passband frequency is pass_freq = 0.9*sample_freq/2 for sample frequencies up to ~ 44.1kHz, and 20kHz
	 * for higher sample frequencies.
	 * <P>
	 * For resampling, the ratio between the clock frequency and the sample frequency is limited as follows: 125*clock_freq/sample_freq < 16384 E.g. provided a clock frequency of ~ 1MHz, the sample
	 * frequency can not be set lower than ~ 8kHz. A lower sample frequency would make the resampling code overfill its 16k sample ring buffer.
	 * <P>
	 * The end of passband frequency is also limited: pass_freq <= 0.9*sample_freq/2
	 * <P>
	 * E.g. for a 44.1kHz sampling rate the end of passband frequency is limited to slightly below 20kHz. This constraint ensures that the FIR table is not overfilled.
	 * 
	 * @param clockFrequency System clock frequency at Hz
	 * @param method sampling method to use
	 * @param samplingFrequency Desired output sampling rate
	 * @return success
	 */
	synchronized public void setSamplingParameters(final double clockFrequency, final double samplingFrequency) {
		filter6581.setClockFrequency(clockFrequency);
		filter8580.setClockFrequency(clockFrequency);

		cyclesPerSample = (float) (clockFrequency / samplingFrequency);
	}

	private void ageBusValue(final int n) {
		if (busValueTtl != 0) {
			busValueTtl -= n;
			if (busValueTtl <= 0) {
				busValue = 0;
				busValueTtl = 0;
			}
		}
	}

	/**
	 * SID clocking - 1 cycle.
	 */
	private short clock() {
		/* clock waveform generators */
		voice[0].wave.clock();
		voice[1].wave.clock();
		voice[2].wave.clock();

		/* emulate SYNC bit */
		voice[0].wave.synchronize(voice[1].wave, voice[2].wave);
		voice[1].wave.synchronize(voice[2].wave, voice[0].wave);
		voice[2].wave.synchronize(voice[0].wave, voice[1].wave);

		/* clock envelope generators */
		voice[0].envelope.clock();
		voice[1].envelope.clock();
		voice[2].envelope.clock();

		return (short) ((voice[0].output(voice[2].wave) + voice[1].output(voice[0].wave) + voice[2].output(voice[1].wave)) >> 7);
	}

	/**
	 * Clock SID forward using chosen output sampling algorithm.
	 * 
	 * @param delta_t c64 clocks to clock
	 * @param buf audio output buffer
	 * @param pos where to begin audio writing
	 * @return
	 */
	public final int clock(final int delta_t, final short buf[], final int pos) {
		ageBusValue(delta_t);
		int res = clockDecimate(delta_t, buf, pos);
		filter.zeroDenormals();
		return res;
	}

	/**
	 * SID clocking with audio sampling - cycle based with linear sample
	 * interpolation.
	 * <P>
	 * Here the chip is clocked every cycle. This yields higher quality sound since the samples are linearly interpolated, and since the external filter attenuates frequencies above 16kHz, thus
	 * reducing sampling noise.
	 * 
	 * @return number of samples constructed
	 */
	private int clockDecimate(int cycles, final short[] buf, final int pos) {
		int s = 0;
		int i;

		for (;;) {
			final float nextSampleOffset = sampleOffset + cyclesPerSample;
			final int cyclesToNextSample = (int) nextSampleOffset;
			if (cyclesToNextSample > cycles) {
				break;
			}
			for (i = 0; i < cyclesToNextSample-1; i++) {
				clock();
			}

			cycles -= cyclesToNextSample;
			sampleOffset = nextSampleOffset - cyclesToNextSample;

			buf[pos + s++] = clock();
		}
		for (i = 0; i < cycles; i++) {
			clock();
		}
		sampleOffset -= cycles;
		return s;
	}

	public Filter6581 getFilter6581() {
		return filter6581;
	}

	public Filter8580 getFilter8580() {
		return filter8580;
	}
}
