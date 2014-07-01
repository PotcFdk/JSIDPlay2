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
package residfp_builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import resid_builder.resid.ChipModel;
import resid_builder.resid.SamplingMethod;
import residfp_builder.ReSIDBuilder.MixerEvent;
import residfp_builder.resid.SID;
import sidplay.ini.IniReader;
import sidplay.ini.intf.IConfig;

public class ReSID extends SIDEmu {
	private static final Logger RESID = Logger.getLogger(ReSID.class.getName());

	private static final String VERSION = "0.0.2";

	private static final int OUTPUTBUFFERSIZE = 5000; /*
													 * supports 5 ms chunk at 96
													 * kHz
													 */

	private final SID sid;

	protected int bufferpos;

	protected float[] buffer;

	private final ReSIDBuilder.MixerEvent mixerEvent;

	public ReSID(EventScheduler context, MixerEvent mixerEvent) {
		super(context);
		this.mixerEvent = mixerEvent;
		sid = new SID();
		buffer = new float[OUTPUTBUFFERSIZE];
		bufferpos = 0;
		reset((byte) 0);
	}

	/** Name of our config file. */
	private static final String FILE_NAME = "residfp.ini";

	@Override
	public void setFilter(IConfig config) {
		try (InputStream is = getClass().getResourceAsStream(
				"/residfp_builder/" + FILE_NAME)) {
			System.out.println("ReSIDfp: Use internal INI file: " + FILE_NAME);
			IniReader iniReader = new IniReader(is);
			FilterConfig filter6581 = FilterConfig.read(iniReader,
					"FilterAlankila6581R4AR_3789");
			FilterConfig filter8580 = FilterConfig.read(iniReader,
					"FilterTrurl8580R5_3691");
			filter(filter6581, filter8580);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void filter(final FilterConfig filter6581,
			final FilterConfig filter8580) {
		if (filter6581 != null) {
			sid.getFilter6581().setCurveProperties(
					filter6581.getBaseresistance(), filter6581.getOffset(),
					filter6581.getSteepness(),
					filter6581.getMinimumfetresistance());
			sid.getFilter6581().setDistortionProperties(
					filter6581.getAttenuation(), filter6581.getNonlinearity(),
					filter6581.getResonanceFactor());
			sid.set6581VoiceNonlinearity(filter6581.getVoiceNonlinearity());
			sid.getFilter6581().setNonLinearity(
					filter6581.getVoiceNonlinearity());
		} else {
			sid.getFilter6581().setCurveAndDistortionDefaults();
		}

		if (filter8580 != null) {
			sid.getFilter8580().setCurveProperties(filter8580.getK(),
					filter8580.getB(), 0, 0);
			sid.getFilter8580().setDistortionProperties(0, 0,
					filter8580.getResonanceFactor());
		} else {
			sid.getFilter8580().setCurveAndDistortionDefaults();
		}
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		sid.reset();
		sid.write(0x18, volume);
		/*
		 * No matter how many chips are in use, mixerEvent is singleton with
		 * respect to them. Only one will be scheduled. This is a bit dirty,
		 * though.
		 */
		context.cancel(mixerEvent);
		mixerEvent.setContext(context);
		context.schedule(mixerEvent, 0, Event.Phase.PHI2);
	}

	@Override
	public byte read(int addr) {
		addr &= 0x1f;
		clock();
		return sid.read(addr);
	}

	@Override
	public void write(int addr, final byte data) {
		addr &= 0x1f;
		super.write(addr, data);
		if (RESID.isLoggable(Level.FINE)) {
			RESID.fine(String.format("write 0x%02x=0x%02x", addr, data));
		}

		clock();
		sid.write(addr, data);
	}

	@Override
	public void clock() {
		int cycles = clocksSinceLastAccess();
		bufferpos += sid.clock(cycles, buffer, bufferpos);
	}

	@Override
	public void setFilterEnable(final boolean enable) {
		sid.getFilter6581().enable(enable);
		sid.getFilter8580().enable(enable);
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
		sid.mute(num, mute);
	}

	@Override
	public void setSampling(final double systemClock, final float freq,
			final SamplingMethod method) {
		sid.setSamplingParameters(systemClock, method, freq, 20000);
	}

	/**
	 * Set the emulated SID model
	 * 
	 * @param model
	 */
	public void setChipModel(final ChipModel model) {
		sid.setChipModel(model);
	}

	@Override
	public ChipModel getChipModel() {
		return sid.getChipModel();
	}

	@Override
	public void input(int input) {
		sid.input(input);
	}

	// Standard component functions
	public static final String credits() {
		String m_credit = "ReSID V" + VERSION + " Engine:\n";
		m_credit += "\tCopyright (C) 1999-2002 Simon White <sidplay2@yahoo.com>\n";
		m_credit += "MOS6581/8580 (SID) Emulation:\n";
		m_credit += "\tCopyright (C) 1999-2004 Dag Lem <resid@nimrod.no>\n";
		m_credit += "\tCopyright (C) 2005-2010 Antti S. Lankila <alankila@bel.fi>\n";
		return m_credit;
	}

}
