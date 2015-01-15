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

import java.util.HashMap;
import java.util.Map;

import libsidplay.common.ChipModel;
import libsidplay.common.SIDChip;
import libsidplay.common.SamplingMethod;

public class SID implements SIDChip {
	private static final int INPUTDIGIBOOST = -0x9500;
	
	/**
	 * Cache for caching the expensive FIR table computation results in the Java
	 * process.
	 */
	private static Map<String, float[][]> FIR_CACHE = new HashMap<String, float[][]>();

	/**
	 * Output scaler.
	 */
	private static final float OUTPUT_LEVEL = 1 / (2047.f * 255.f * 3.0f * 2.0f);

	/** SID voices */
	public final Voice[] voice = new Voice[] { new Voice(), new Voice(),
			new Voice() };

	/** Currently active filter */
	private Filter filter;

	/**
	 * Get currently active filter.
	 * 
	 * @return currently active filter
	 */
	public final Filter getFilter() {
		return filter;
	}

	/** Filter used, if model is set to 6581 */
	private final Filter6581 filter6581 = new Filter6581();

	/** Filter used, if model is set to 8580 */
	private final Filter8580 filter8580 = new Filter8580();

	/**
	 * External filter that provides high-pass and low-pass filtering to adjust
	 * sound tone slightly.
	 */
	private final ExternalFilter externalFilter = new ExternalFilter();

	/** Paddle X register support */
	private final Potentiometer potX = new Potentiometer();

	/** Paddle Y register support */
	private final Potentiometer potY = new Potentiometer();

	/** Last written value */
	private byte busValue;

	/** Time to live for the last written value */
	private int busValueTtl;

	/** External audio input. */
	private float ext_in;

	/** Maximum convolution length */
	private static final int RINGSIZE = 2048;

	/**
	 * Aliasing parameter: we don't care about accurate sound reproduction above
	 * this frequency. The lower this is, the faster resampling will be, but the
	 * worse it will sound.
	 */
	private static final float MAXIMUM_AUDIBLE_FREQUENCY = 20000;

	/** Currently active sampling method. */
	private SamplingMethod samplingMethod;

	/** Interval between samples. (Not a whole integer.) */
	private float cyclesPerSample;

	/** Convolution length for SINC resampling. */
	private int firN;

	/** Number of SINC subphases (for subclock sampling) */
	private int firRES;

	/** The subclock [0, 1[ for precise subphase sampling. */
	private float sampleOffset;

	/** Currently active FIR table. */
	private float fir[][];

	/** Ring buffer with overflow for contiguous storage of RINGSIZE samples. */
	private final float sample[] = new float[RINGSIZE * 2];

	/** Index of next unused sample in ring buffer. */
	private int sampleIndex;

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
	 * Estimate DAC nonlinearity. The SID contains R-2R ladder, but the second
	 * resistor is not exactly double the first. The parameter nonLinearity
	 * models the deviation from the resistor lengths. There appears to be about
	 * 4 % error on the 6581, resulting in major kinks on the DAC. The value
	 * that the DAC yields tends to be larger than expected. The output of this
	 * method is normalized such that DAC errors occur both above and below the
	 * ideal value equally.
	 * 
	 * @param input
	 *            digital value to convert to analog
	 * @param nonLinearity
	 *            nonlinearity parameter, 1.0 for perfect linearity.
	 * @param maxBit
	 *            highest bit that may be set in input.
	 * @return the analog value as modeled from the R-2R network.
	 */
	public static float kinkedDac(final int input, final float nonLinearity,
			final int maxBit) {
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
	 * @param count
	 *            chip number
	 */
	public SID() {
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
	 * @param model
	 *            chip model to use
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
			throw new RuntimeException("Don't know how to handle chip type "
					+ model);
		}

		/* calculate waveform-related tables, feed them to the generator */
		final Object[] tables = WaveformCalculator.rebuildWftable(model,
				nonLinearity);

		/* update voice offsets */
		for (int i = 0; i < 3; i++) {
			voice[i].setChipModel(model);
			voice[i].envelope.setNonLinearity(nonLinearity);
			voice[i].wave.setWftable((float[][]) tables[0],
					(float[]) tables[1], (byte[][]) tables[2]);
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
		externalFilter.reset();

		busValue = 0;
		busValueTtl = 0;
	}

	/**
	 * 16-bit input (EXT IN). Write 16-bit sample to audio input. NB! The caller
	 * is responsible for keeping the value within 16 bits. Note that to mix in
	 * an external audio signal, the signal should be resampled to 1MHz first to
	 * avoid sampling noise.
	 * 
	 * @param value
	 *            input level to set
	 */
	public void input(final int value) {
		// Voice outputs are 20 bits. Scale up to match three voices in order
		// to facilitate simulation of the MOS8580 "digi boost" hardware hack.
		ext_in = (value << 4) * 3;
	}

	/**
	 * Read registers.
	 * <P>
	 * Reading a write only register returns the last byte written to any SID
	 * register. The individual bits in this value start to fade down towards
	 * zero after a few cycles. All bits reach zero within approximately $2000 -
	 * $4000 cycles. It has been claimed that this fading happens in an orderly
	 * fashion, however sampling of write only registers reveals that this is
	 * not the case. NB! This is not correctly modeled. The actual use of write
	 * only registers has largely been made in the belief that all SID registers
	 * are readable. To support this belief the read would have to be done
	 * immediately after a write to the same register (remember that an
	 * intermediate write to another register would yield that value instead).
	 * With this in mind we return the last value written to any SID register
	 * for $2000 cycles without modeling the bit fading.
	 * 
	 * @param offset
	 *            SID register to read
	 * @return value read from chip
	 */
	public byte read(final int offset) {
		switch (offset) {
		case 0x19:
			return potX.readPOT();
		case 0x1a:
			return potY.readPOT();
		case 0x1b:
			return model == ChipModel.MOS6581 ? voice[2].wave
					.readOSC6581(voice[0].wave) : voice[2].wave
					.readOSC8580(voice[0].wave);
		case 0x1c:
			return voice[2].envelope.readENV();
		default:
			return busValue;
		}
	}

	/**
	 * Write registers.
	 * 
	 * @param offset
	 *            chip register to write
	 * @param value
	 *            value to write
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
	 * @param channel
	 *            channe to modify
	 * @param enable
	 *            is muted?
	 */
	public void mute(final int channel, final boolean enable) {
		voice[channel].mute(enable);
	}

	/** Maximum error acceptable in I0 is 1e-6, or ~96 dB. */
	private static final double I0E = 1e-6;

	/**
	 * I0() computes the 0th order modified Bessel function of the first kind.
	 * This function is originally from resample-1.5/filterkit.c by J. O. Smith.
	 * It is used to build the Kaiser window for resampling.
	 * 
	 * @param x
	 *            evaluate I0 at x
	 * @return value of I0 at x.
	 */
	private static double I0(final double x) {
		double sum = 1, u = 1, n = 1;
		final double halfx = x / 2;

		do {
			final double temp = halfx / n;
			u *= temp * temp;
			sum += u;
			n += 1;
		} while (u >= I0E * sum);

		return sum;
	}

	/**
	 * Setting of SID sampling parameters.
	 * <P>
	 * Use a clock freqency of 985248Hz for PAL C64, 1022730Hz for NTSC C64. The
	 * default end of passband frequency is pass_freq = 0.9*sample_freq/2 for
	 * sample frequencies up to ~ 44.1kHz, and 20kHz for higher sample
	 * frequencies.
	 * <P>
	 * For resampling, the ratio between the clock frequency and the sample
	 * frequency is limited as follows: 125*clock_freq/sample_freq < 16384 E.g.
	 * provided a clock frequency of ~ 1MHz, the sample frequency can not be set
	 * lower than ~ 8kHz. A lower sample frequency would make the resampling
	 * code overfill its 16k sample ring buffer.
	 * <P>
	 * The end of passband frequency is also limited: pass_freq <=
	 * 0.9*sample_freq/2
	 * <P>
	 * E.g. for a 44.1kHz sampling rate the end of passband frequency is limited
	 * to slightly below 20kHz. This constraint ensures that the FIR table is
	 * not overfilled.
	 * 
	 * @param clockFrequency
	 *            System clock frequency at Hz
	 * @param method
	 *            sampling method to use
	 * @param samplingFrequency
	 *            Desired output sampling rate
	 * @return success
	 */
	synchronized public void setSamplingParameters(final double clockFrequency,
			final SamplingMethod method, final double samplingFrequency,
			final double highestAccurateFrequency) {
		filter6581.setClockFrequency(clockFrequency);
		filter8580.setClockFrequency(clockFrequency);
		externalFilter.setClockFrequency(clockFrequency);

		cyclesPerSample = (float) (clockFrequency / samplingFrequency);

		sampleOffset = 0;

		// Clear sample buffer.
		for (int j = 0; j < RINGSIZE * 2; j++) {
			sample[j] = 0;
		}

		if (method != SamplingMethod.RESAMPLE) {
			samplingMethod = method;
			return;
		}

		/* rest of the code initializes the FIR. */
		sampleIndex = 0;

		/*
		 * Allow specifying at most 90 % of passband to limit the CPU time spent
		 * on resampling.
		 */
		if (2 * highestAccurateFrequency / samplingFrequency > 0.95f) {
			throw new RuntimeException(
					"Requested passband is too narrow. Try raising sampling frequency or lowering highest accurate frequency.");
		}

		// 16 bits -> -96dB stopband attenuation.
		final double A = -20 * Math.log10(1.0 / (1 << 16));

		// For calculation of beta and N see the reference for the kaiserord
		// function in the MATLAB Signal Processing Toolbox:
		// http://www.mathworks.com/access/helpdesk/help/toolbox/signal/kaiserord.html
		final double beta = 0.1102 * (A - 8.7);
		final double I0beta = I0(beta);
		final double halfCyclesPerSample = clockFrequency / samplingFrequency
				/ 2;

		/*
		 * Widen the transition band to allow aliasing down to the specified
		 * highest correctly reproduced frequency. I sincerely hope that
		 * highestAccurateFrequency is above 20 kHz, or this will sound like
		 * shit.
		 */
		double aliasingAllowance = samplingFrequency / 2
				- MAXIMUM_AUDIBLE_FREQUENCY;
		if (aliasingAllowance < 0) {
			aliasingAllowance = 0;
		}

		final double transitionBandwidth = samplingFrequency / 2
				- highestAccurateFrequency + aliasingAllowance;
		{
			// The filter order will maximally be 124 with the current
			// constraints.
			// N >= (96.33 - 7.95)/(2 * pi * 2.285 * (maxfreq - passbandfreq) >=
			// 123
			// The filter order is equal to the number of zero crossings, i.e.
			// it should be an even number (sinc is symmetric about x = 0).
			//
			// XXX: analysis indicates that the filter is slighly overspecified
			// by
			// there constraints. Need to check why. One possibility is the
			// level of audio being in truth closer to 15-bit than 16-bit.
			int N = (int) ((A - 7.95)
					/ (2 * Math.PI * 2.285 * transitionBandwidth / samplingFrequency) + 0.5);
			N += N & 1;

			// The filter length is equal to the filter order + 1.
			// The filter length must be an odd number (sinc is symmetric about
			// x = 0).
			firN = (int) (N * halfCyclesPerSample) + 1;
			firN |= 1;

			// Check whether the sample ring buffer would overflow.
			if (firN > RINGSIZE - 1) {
				throw new RuntimeException("FIR ring buffer would overflow");
			}

			/*
			 * Error is bound by 1.234 / L^2, so for 16-bit: sqrt(1.234 * (1 <<
			 * 16))
			 */
			firRES = (int) (Math.sqrt(1.234 * (1 << 16)) / halfCyclesPerSample + 0.5);
		}

		// The cutoff frequency is midway through the transition band
		final double wc = (highestAccurateFrequency + transitionBandwidth / 2)
				/ samplingFrequency * Math.PI * 2;

		final String firKey = firN + "," + firRES + "," + wc + ","
				+ halfCyclesPerSample;
		fir = FIR_CACHE.get(firKey);

		/*
		 * The FIR computation is expensive and we set sampling parameters
		 * often, but from a very small set of choices. Thus, caching this seems
		 * appropriate.
		 */
		if (fir == null) {
			// Allocate memory for FIR tables.
			fir = new float[firRES][firN];
			FIR_CACHE.put(firKey, fir);

			/* Calculate the sinc tables. */
			final double scale = wc / halfCyclesPerSample / Math.PI;
			for (int i = 0; i < firRES; i++) {
				final double jPhase = (double) i / firRES + firN / 2;
				for (int j = 0; j < firN; j++) {
					final double x = j - jPhase;

					final double xt = x / (firN / 2);
					final double kaiserXt = Math.abs(xt) < 1 ? I0(beta
							* Math.sqrt(1 - xt * xt))
							/ I0beta : 0;

					final double wt = wc * x / halfCyclesPerSample;
					final double sincWt = Math.abs(wt) >= 1e-8 ? Math.sin(wt)
							/ wt : 1;

					fir[i][j] = (float) (scale * sincWt * kaiserXt);
				}
			}
		}
		// This must be the last statement to ensure,
		// that the FIR table has been build completely,
		// because this is called from UI thread as well.
		samplingMethod = method;
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
	private float clock() {
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

		return externalFilter.clock(filter.clock(
				voice[0].output(voice[2].wave), voice[1].output(voice[0].wave),
				voice[2].output(voice[1].wave), ext_in))
				* OUTPUT_LEVEL;
	}

	/**
	 * Clock SID forward using chosen output sampling algorithm.
	 * 
	 * @param delta_t
	 *            c64 clocks to clock
	 * @param buf
	 *            audio output buffer
	 * @param pos
	 *            where to begin audio writing
	 * @return
	 */
	public final int clock(final int /* cycle_count */delta_t,
			final float buf[], final int pos) {
		ageBusValue(delta_t);

		int res;
		switch (samplingMethod) {
		default:
		case DECIMATE:
			res = clockDecimate(delta_t, buf, pos);
			break;
		case RESAMPLE:
			res = clockResampleInterpolate(delta_t, buf, pos);
			break;
		}
		filter.zeroDenormals();
		externalFilter.zeroDenormals();
		return res;
	}

	/**
	 * Clock SID forward with no audio production. This trashes the SID state,
	 * and this method can't be used together with audio-producing clock_xxx
	 * methods.
	 * 
	 * @param delta_t
	 *            c64 clocks to clock.
	 * @return
	 */
	public void clockSilent(final int delta_t) {
		ageBusValue(delta_t);

		for (int i = 0; i < delta_t; i++) {
			/* clock waveform generators */
			voice[0].wave.clock();
			voice[1].wave.clock();
			voice[2].wave.clock();

			/* emulate SYNC bit */
			voice[0].wave.synchronize(voice[1].wave, voice[2].wave);
			voice[1].wave.synchronize(voice[2].wave, voice[0].wave);
			voice[2].wave.synchronize(voice[0].wave, voice[1].wave);

			/* clock ENV3 only */
			// voice[0].envelope.clock();
			// voice[1].envelope.clock();
			voice[2].envelope.clock();
		}
	}

	/**
	 * SID clocking with audio sampling - cycle based with linear sample
	 * interpolation.
	 * <P>
	 * Here the chip is clocked every cycle. This yields higher quality sound
	 * since the samples are linearly interpolated, and since the external
	 * filter attenuates frequencies above 16kHz, thus reducing sampling noise.
	 * 
	 * @return number of samples constructed
	 */
	private int clockDecimate(int cycles, final float buf[], final int pos) {
		int s = 0;
		int i;

		for (;;) {
			final float nextSampleOffset = sampleOffset + cyclesPerSample;
			final int cyclesToNextSample = (int) nextSampleOffset;
			if (cyclesToNextSample > cycles) {
				break;
			}
			for (i = 0; i < cyclesToNextSample - 1; i++) {
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

	private static float convolve(final float a[], int aPos, final float b[]) {
		float out = 0.f;
		for (int i = 0; i < b.length; i++, aPos++) {
			out += a[aPos] * b[i];
		}
		return out;
	}

	/**
	 * SID clocking with audio sampling - cycle based with audio resampling.
	 * <P>
	 * This is the theoretically correct (and computationally intensive) audio
	 * sample generation. The samples are generated by resampling to the
	 * specified sampling frequency. The work rate is inversely proportional to
	 * the percentage of the bandwidth allocated to the filter transition band.
	 * <P>
	 * This implementation is based on the paper
	 * "A Flexible Sampling-Rate Conversion Method", by J. O. Smith and P.
	 * Gosset, or rather on the expanded tutorial on the
	 * "Digital Audio Resampling Home Page":
	 * http:*www-ccrma.stanford.edu/~jos/resample/
	 * <P>
	 * By building shifted FIR tables with samples according to the sampling
	 * frequency, this implementation dramatically reduces the computational
	 * effort in the filter convolutions, without any loss of accuracy. The
	 * filter convolutions are also vectorizable on current hardware.
	 * <P>
	 * Further possible optimizations are:
	 * <OL>
	 * <LI>An equiripple filter design could yield a lower filter order, see
	 * http://www.mwrf.com/Articles/ArticleID/7229/7229.html
	 * <LI>The Convolution Theorem could be used to bring the complexity of
	 * convolution down from O(n*n) to O(n*log(n)) using the Fast Fourier
	 * Transform, see http://en.wikipedia.org/wiki/Convolution_theorem
	 * <LI>Simply resampling in two steps can also yield computational savings,
	 * since the transition band will be wider in the first step and the
	 * required filter order is thus lower in this step. Laurent Ganier has
	 * found the optimal intermediate sampling frequency to be (via derivation
	 * of sum of two steps):<BR>
	 * <CODE>2 * pass_freq + sqrt [ 2 * pass_freq * orig_sample_freq
	 *       * (dest_sample_freq - 2 * pass_freq) / dest_sample_freq ]</CODE>
	 * </OL>
	 * 
	 * @return number of samples constructed
	 */
	private int clockResampleInterpolate(int cycles, final float buf[],
			final int pos) {
		int s = 0;

		for (;;) {
			final float nextSampleOffset = sampleOffset + cyclesPerSample;
			/* full clocks left to next sample */
			final int cyclesToNextSample = (int) nextSampleOffset;
			if (cyclesToNextSample > cycles) {
				break;
			}

			/* clock forward delta_t_sample samples */
			for (int i = 0; i < cyclesToNextSample; i++) {
				sample[sampleIndex] = sample[sampleIndex + RINGSIZE] = clock();
				sampleIndex = sampleIndex + 1 & RINGSIZE - 1;
			}
			cycles -= cyclesToNextSample;

			/* Phase of the sample in terms of clock, [0 .. 1[. */
			sampleOffset = nextSampleOffset - cyclesToNextSample;

			/* find the first of the nearest fir tables close to the phase */
			float firTableOffset = sampleOffset * firRES;
			int firTableFirst = (int) firTableOffset;
			/* [0 .. 1[ */
			firTableOffset -= firTableFirst;

			/*
			 * find firN most recent samples, plus one extra in case the FIR
			 * wraps.
			 */
			int sampleStart = sampleIndex - firN + RINGSIZE - 1;

			final float v1 = convolve(sample, sampleStart, fir[firTableFirst]);
			// Use next FIR table, wrap around to first FIR table using
			// previous sample.
			if (++firTableFirst == firRES) {
				firTableFirst = 0;
				++sampleStart;
			}

			final float v2 = convolve(sample, sampleStart, fir[firTableFirst]);

			// Linear interpolation between the sinc tables yields good
			// approximation for the exact value.
			buf[pos + s++] = v1 + firTableOffset * (v2 - v1);
		}

		/* clock forward delta_t samples */
		for (int i = 0; i < cycles; i++) {
			sample[sampleIndex] = sample[sampleIndex + RINGSIZE] = clock();
			sampleIndex = sampleIndex + 1 & RINGSIZE - 1;
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

	@Override
	public int clock(int piece, int[] audioBuffer, int offset) {
		float[] buffer = new float[audioBuffer.length];
		final int clock = clock(piece, buffer, offset);
		for (int i = offset; i < buffer.length; i++) {
			audioBuffer[i] = Math.round(buffer[i] * 32768f);
		}
		return clock;
	}

	@Override
	public int getInputDigiBoost() {
		return model.equals(ChipModel.MOS8580) ? INPUTDIGIBOOST : 0;
	}
}
