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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import libsidplay.common.ChipModel;

/**
 * Combined waveform calculator for WaveformGenerator.
 * 
 * @author Antti Lankila
 */
public final class WaveformCalculator {
	private static final Map<String, float[][]> WFTABLE_CACHE = new HashMap<String, float[][]>();
	private static final Map<String, byte[][]> WFDIGITAL_CACHE = new HashMap<String, byte[][]>();

	private static class CombinedWaveformConfig {
		public CombinedWaveformConfig(final float f, final float g, final float h, final float i, final float j) {
			bias = f;
			pulsestrength = g;
			topbit = h;
			distance = i;
			stmix = j;
		}

		float bias;
		float pulsestrength;
		float topbit;
		float distance;
		float stmix;
	}

	/*
	 * Nata provided me with a sampling that indicates the bit turn on/off threshold is very, very steep. Since this *should* be an analog step, I resist just turning it into a hard cutoff, but my
	 * resolution is wavering. 512 is approximately right for 8580, and hopefully approximates the analog character yet.
	 */
	private static final float sharpness = 512.f;
	/*
	 * the "bits wrong" figures below are not directly comparable. 0 bits are very easy to predict, and waveforms that are mostly zero have low scores. More comparable scores would be found by
	 * dividing with the count of 1-bits, or something.
	 */
	private static final CombinedWaveformConfig wfconfig[][] = new CombinedWaveformConfig[][] {
			{ /* kevtris chip G (6581r2/r3) */
				new CombinedWaveformConfig(0.880815f, 0f, 0f, 0.3279614f, 0.5999545f), // error 1795
				new CombinedWaveformConfig(0.8924618f, 2.014781f, 1.003332f, 0.02992322f, 0.0f), // error 11610
				new CombinedWaveformConfig(0.8646501f, 1.712586f, 1.137704f, 0.02845423f, 0f), // error 21307
				new CombinedWaveformConfig(0.9527834f, 1.794777f, 0f, 0.09806272f, 0.7752482f), // error 196
				new CombinedWaveformConfig(0.5f, 0.0f, 1.0f, 0.0f, 0.0f),
			}, { /* kevtris chip V (8580) */
				new CombinedWaveformConfig(0.9781665f, 0f, 0.9899469f, 8.087667f, 0.8226412f), // error 5546
				new CombinedWaveformConfig(0.9097769f, 2.039997f, 0.9584096f, 0.1765447f, 0f), // error 18763
				new CombinedWaveformConfig(0.9231212f, 2.084788f, 0.9493895f, 0.1712518f, 0f), // error 17103
				new CombinedWaveformConfig(0.9845552f, 1.415612f, 0.9703883f, 3.68829f, 0.8265008f), // error 3319
				new CombinedWaveformConfig(0.5f, 0.0f, 1.0f, 0.0f, 0.0f),
			},
	};

	/* render output from bitstate */
	private static float makeSample(final float[] dac, final float[] o) {
		float out = 0;
		for (int i = 0; i < 12; i++) {
			out += o[i] * dac[i];
		}
		return out;
	}

	private static byte makeDigital(final float[] o) {
		byte out = 0;
		for (int i = 11; i >= 4; i--) {
			out <<= 1;
			if (o[i] > 0.5f) {
				out |= 1;
			}
		}
		return out;
	}

	/**
	 * Build waveform tables for use by WaveformGenerator. The method returns 3
	 * tables in an Object[] wrapper:
	 * 
	 * 1. float[11][4096] wftable: the analog values in the waveform table
	 * 2. float[12] dac table for values of the nonlinear bits used in waveforms.
	 * 3. byte[11][4096] wfdigital: the digital values in the waveform table.
	 * 
	 * The wf* tables are structured as follows: indices 0 .. 6 correspond
	 * to SID waveforms of 1 to 7 with pulse width value set to 0x1000 (never
	 * triggered). Indices 7 .. 10 correspond to the pulse waveforms with
	 * width set to 0x000 (always triggered).
	 * 
	 * @param model Chip model to use
	 * @param nonlinearity Nonlinearity factor for 6581 tables, 1.0 for 8580
	 * @return Table suite
	 */
	protected static Object[] rebuildWftable(ChipModel model, float nonlinearity) {
		float[] dac = new float[12];
		for (int i = 0; i < 12; i++) {
			dac[i] = SID.kinkedDac((1 << i), nonlinearity, 12);
		}

		final String key = nonlinearity + "," + model;
		if (! WFTABLE_CACHE.containsKey(key)) {
			float wave_zero = model == ChipModel.MOS6581 ? -0x380 : -0x800;
		
			final float[] o = new float[12];
			float[][] wftable = new float[11][4096];
			byte[][] wfdigital = new byte[11][4096];
			
			for (int waveform = 1; waveform < 8; waveform++) {
				for (int accumulator = 0; accumulator < 1 << 24; accumulator += 1 << 12) {
					/*
				 	* generate pulse-low variants. Also, when waveform < 4, pw doesn't matter.
				 	*/
					fillInWaveformSample(o, model, waveform, accumulator, 0x1000);
					wftable[waveform - 1][accumulator >> 12] = makeSample(dac, o) + wave_zero;
				
					wfdigital[waveform - 1][accumulator >> 12] = makeDigital(o);
					/* Add pulse-high variants after pulse-low state variants */
					if (waveform >= 4) {
						fillInWaveformSample(o, model, waveform, accumulator, 0x000);
						wftable[waveform + 3][accumulator >> 12] = makeSample(dac, o) + wave_zero;
						wfdigital[waveform + 3][accumulator >> 12] = makeDigital(o);
					}
				}
			}

			WFTABLE_CACHE.put(key, wftable);
			WFDIGITAL_CACHE.put(key, wfdigital);
		}

		return new Object[] { WFTABLE_CACHE.get(key), dac, WFDIGITAL_CACHE.get(key) };
	}

	/* explode reg12 to a floating point bit array */
	private static void populate(final int v, final float[] o) {
		int j = 1;
		for (int i = 0; i < 12; i++) {
			o[i] = (v & j) != 0 ? 1.f : 0.f;
			j <<= 1;
		}
	}

	/* waveform values valid are 1 .. 7 */
	private static void fillInWaveformSample(final float[] o, ChipModel model, int waveform, int accumulator, int pw) {
		int i;

		/* P */
		if (waveform == 4) {
			populate(accumulator >> 12 >= pw ? 0xfff : 0x000, o);
			return;
		}

		final CombinedWaveformConfig config = wfconfig[model == ChipModel.MOS6581 ? 0 : 1][waveform == 3 ? 0 : waveform == 5 ? 1 : waveform == 6 ? 2 : waveform == 7 ? 3 : 4];

		/* S with strong top bit for 6581 */
		populate(accumulator >> 12, o);

		/* convert to T */
		if ((waveform & 3) == 1) {
			final boolean top = (accumulator & 0x800000) != 0;
			for (i = 11; i > 0; i--) {
				if (top) {
					o[i] = 1.0f - o[i - 1];
				} else {
					o[i] = o[i - 1];
				}
			}
			o[0] = 0;
		}

		/* convert to ST */
		if ((waveform & 3) == 3) {
			/* bottom bit is grounded via T waveform selector */
			o[0] *= config.stmix;
			for (i = 1; i < 12; i++) {
				o[i] = o[i - 1] * (1.f - config.stmix) + o[i] * config.stmix;
			}
		}

		o[11] *= config.topbit;

		/* ST, P* waveform? */
		if (waveform == 3 || waveform > 4) {
			final float distancetable[] = new float[12 * 2 + 1];
			for (i = 0; i <= 12; i++) {
				distancetable[12 + i] = distancetable[12 - i] = 1.f / (1.f + i * i * config.distance);
			}

			float pulse = accumulator >> 12 >= pw ? 1.f : -1.f;
			pulse *= config.pulsestrength;

			final float tmp[] = new float[12];
			for (i = 0; i < 12; i++) {
				float avg = 0;
				float n = 0;
				for (int j = 0; j < 12; j++) {
					final float weight = distancetable[i - j + 12];
					avg += o[j] * weight;
					n += weight;
				}
				/* pulse control bit */
				if (waveform > 4) {
					final float weight = distancetable[i - 12 + 12];
					avg += pulse * weight;
					n += weight;
				}

				tmp[i] = (o[i] + avg / n) * 0.5f;
			}

			for (i = 0; i < 12; i++) {
				o[i] = tmp[i];
			}
		}

		/*
		 * Use the environment around bias value to set/clear dac bit. Measurements indicate the threshold is very sharp.
		 */
		for (i = 0; i < 12; i++) {
			o[i] = (o[i] - config.bias) * sharpness;

			o[i] += 0.5f;
			if (o[i] > 1.f) {
				o[i] = 1.f;
			}
			if (o[i] < 0.f) {
				o[i] = 0.f;
			}
		}
	}

	public static void main(String[] args) {
		Object[] data6581 = rebuildWftable(ChipModel.MOS6581, 0.96f);
		Object[] data8580 = rebuildWftable(ChipModel.MOS8580, 1.0f);

		dump("MOS6581", data6581);
		dump("MOS8580", data8580);
	}

	private static void dump(String basename, Object[] data) {
		float[][] wftable = (float[][]) data[0];
		float[] dac = (float[]) data[1];
		byte[][] digitable = (byte[][]) data[2];

		String dir = "c:/Users/AL/Desktop/";

		try {
			DataOutputStream output;
			
			output = new DataOutputStream(
					new FileOutputStream(dir + basename + "_wave.dat", false));
			for (int i = 0; i < wftable.length; i++) {
				for (int j = 0; j < wftable[i].length; j++) {
					output.writeInt(Math.round(wftable[i][j]));
				}
			}
			output.close();
			
			output = new DataOutputStream(
					new FileOutputStream(dir + basename + "_dac.dat", false));
			for (int i = 0; i < dac.length; i++) {
				output.writeInt(Math.round(dac[i]));
			}
			output.close();
			
			output = new DataOutputStream(
					new FileOutputStream(dir + basename + "_digi.dat", false));
			for (int i = 0; i < digitable.length; i++) {
				for (int j = 0; j < digitable[i].length; j++) {
					output.writeByte(digitable[i][j]);
				}
			}
			output.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
