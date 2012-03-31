/**
 *                           ReSid Emulation
 *                           ---------------
 *  begin                : Fri Apr 4 2001
 *  copyright            : (C) 2001 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package resid_builder;

import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.ISID2Types.sid2_model_t;
import resid_builder.resid.ISIDDefs.ChipModel;
import resid_builder.resid.SID;

public class ReSID {
	private static final Logger RESID = Logger.getLogger(ReSID.class.getName());

	private static final int OUTPUTBUFFERSIZE = 5000; /* supports 5 ms chunk at 96 kHz */

	private final Phase m_phase = Phase.PHI2;

	private final SID m_sid;

	private int m_accessClk;

	private final short[] m_buffer;
	
	private int m_bufferpos;
	
	private final EventScheduler m_context;
	
	public ReSID(EventScheduler context) {
		m_context = context;
		m_sid = new SID();
		m_buffer = new short[OUTPUTBUFFERSIZE];
		m_bufferpos = 0;
		reset((byte) 0);
	}

	public void filter(final FilterConfig filter6581, final FilterConfig filter8580) {
		if (filter6581 != null) {
			m_sid.getFilter6581().setCurveProperties(filter6581.getBaseresistance(), filter6581.getOffset(), filter6581.getSteepness(), filter6581.getMinimumfetresistance());
			m_sid.getFilter6581().setDistortionProperties(filter6581.getAttenuation(), filter6581.getNonlinearity(), filter6581.getResonanceFactor());
			m_sid.set6581VoiceNonlinearity(filter6581.getVoiceNonlinearity());
			m_sid.getFilter6581().setNonLinearity(filter6581.getVoiceNonlinearity());
		} else {
			m_sid.getFilter6581().setCurveAndDistortionDefaults();
		}

		if (filter8580 != null) {
			m_sid.getFilter8580().setCurveProperties(filter8580.getK(), filter8580.getB(), 0, 0);
			m_sid.getFilter8580().setDistortionProperties(0, 0, filter8580.getResonanceFactor());
		} else {
			m_sid.getFilter8580().setCurveAndDistortionDefaults();
		}
	}

	public void reset(final byte volume) {
		m_accessClk = 0;
		m_sid.reset();
		m_sid.write(0x18, volume);
	}

	public byte read(final int addr) {
		clock();
		return m_sid.read(addr);
	}

	public void write(final int addr, final byte data) {
		if (RESID.isLoggable(Level.FINE)) {
			RESID.fine(String.format("write 0x%02x=0x%02x", addr, data));
		}
		clock();
		m_sid.write(addr, data);
	}

	public void clock() {
		final int cycles = m_context.getTime(m_phase) - m_accessClk;
		m_accessClk += cycles;
		m_bufferpos += m_sid.clock(cycles, m_buffer, m_bufferpos);
	}

	public void filter(final boolean enable) {
		m_sid.getFilter6581().enable(enable);
		m_sid.getFilter8580().enable(enable);
	}

	public void voice(final int num, final boolean mute) {
		m_sid.mute(num, mute);
	}

	public void sampling(final float systemclock, final float freq) {
		m_sid.setSamplingParameters(systemclock, freq);
	}

	/**
	 * Set the emulated SID model
	 * 
	 * @param model
	 */
	public void model(final sid2_model_t model) {
		if (model == sid2_model_t.SID2_MOS8580) {
			m_sid.setChipModel(ChipModel.MOS8580);
		} else {
			m_sid.setChipModel(ChipModel.MOS6581);
		}
	}

	public SID sid() {
		return m_sid;
	}

	public short[] buffer() {
		return m_buffer;
	}
	
	public int bufferpos() {
		return m_bufferpos;
	}
	
	public void bufferpos(int newvalue) {
		m_bufferpos = newvalue;
	}
}
