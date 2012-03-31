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

import java.io.DataInputStream;

import resid_builder.resid.ISIDDefs.ChipModel;

/**
 * Combined waveform calculator for WaveformGenerator.
 * This class is really a sham, see jsidplay2's corresponding class 
 * 
 * @author Antti Lankila
 */
public final class WaveformCalculator {
	/**
	 * Build waveform tables for use by WaveformGenerator. The method returns 3
	 * tables in an Object[] wrapper:
	 * 
	 * 1. float[11][4096] wftable: the analog values in the waveform table 2.
	 * float[12] dac table for values of the nonlinear bits used in waveforms.
	 * 3. byte[11][4096] wfdigital: the digital values in the waveform table.
	 * 
	 * The wf* tables are structured as follows: indices 0 .. 6 correspond to
	 * SID waveforms of 1 to 7 with pulse width value set to 0x1000 (never
	 * triggered). Indices 7 .. 10 correspond to the pulse waveforms with width
	 * set to 0x000 (always triggered).
	 * 
	 * @param model
	 *            Chip model to use
	 * @return Table suite
	 */
	protected static Object[] rebuildWftable(ChipModel model) {
		/* XXX: why do I need a path here? The relative reference should just work. */
		return load("resid_builder/resid/" + model.toString());
	}
	
	private static final Object[] load(String basename) {
		try {
			ClassLoader cl = WaveformCalculator.class.getClassLoader();
			DataInputStream dis;

			int[][] wave = new int[11][4096];
			int[] dac = new int[12];
			byte[][] digi = new byte[11][4096];

			dis = new DataInputStream(cl.getResourceAsStream(basename + "_wave.dat"));
			for (int i = 0; i < 11; i ++) {
				for (int j = 0; j < 4096; j ++) {
					wave[i][j] = dis.readInt();
				}
			}
			dis.close();
			
			dis = new DataInputStream(cl.getResourceAsStream(basename + "_dac.dat"));
			for (int i = 0; i < 12; i ++) {
				dac[i] = dis.readInt();
			}

			dis = new DataInputStream(cl.getResourceAsStream(basename + "_digi.dat"));
			for (int i = 0; i < 11; i ++) {
				for (int j = 0; j < 4096; j ++) {
					digi[i][j] = dis.readByte();
				}
			}
			dis.close();
			
			return new Object[] { wave, dac, digi };
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
