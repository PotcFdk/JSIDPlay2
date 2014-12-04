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

import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.common.SamplingMethod;
import residfp_builder.ReSIDBuilder.MixerEvent;
import residfp_builder.resid.Filter6581;
import residfp_builder.resid.Filter8580;
import residfp_builder.resid.SID;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFilterSection;

public class ReSID extends SIDEmu {
	private static final Logger RESID = Logger.getLogger(ReSID.class.getName());

	/**
	 * Supports 5 ms chunk at 96 kHz
	 */
	private static final int OUTPUTBUFFERSIZE = 5000;

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

	@Override
	public void setFilter(IConfig config, boolean isStereo) {
		final Filter6581 filter6581 = sid.getFilter6581();
		final Filter8580 filter8580 = sid.getFilter8580();

		String filterName6581 = isStereo ? config.getEmulation()
				.getReSIDfpStereoFilter6581() : config.getEmulation()
				.getReSIDfpFilter6581();
		if (filterName6581 == null) {
			filter6581.setCurveAndDistortionDefaults();
		}
		String filterName8580 = isStereo ? config.getEmulation()
				.getReSIDfpStereoFilter8580() : config.getEmulation()
				.getReSIDfpFilter8580();
		if (filterName8580 == null) {
			filter8580.setCurveAndDistortionDefaults();
		}
		for (IFilterSection filter : config.getFilter()) {
			if (filter.getName().equals(filterName6581)
					&& filter.isReSIDfpFilter6581()) {
				filter6581.setCurveProperties(filter.getBaseresistance(),
						filter.getOffset(), filter.getSteepness(),
						filter.getMinimumfetresistance());
				filter6581.setDistortionProperties(filter.getAttenuation(),
						filter.getNonlinearity(), filter.getResonanceFactor());
				sid.set6581VoiceNonlinearity(filter.getVoiceNonlinearity());
				filter6581.setNonLinearity(filter.getVoiceNonlinearity());
			} else if (filter.getName().equals(filterName8580)
					&& filter.isReSIDfpFilter8580()) {
				filter8580.setCurveProperties(filter.getK(), filter.getB(), 0,
						0);
				filter8580.setDistortionProperties(0, 0,
						filter.getResonanceFactor());
			}
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
	 * Gets the {@link SID} instance being used.
	 *
	 * @return The {@link SID} instance being used.
	 */
	public SID sid() {
		return sid;
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
		String m_credit = "MOS6581/8580 (SID) - Antti S. Lankila's resid-fp (distortion simulation):\n";
		m_credit += "\tCopyright (©) 1999-2004 Dag Lem <resid@nimrod.no>\n";
		m_credit += "\tCopyright (©) 2005-2011 Antti S. Lankila <alankila@bel.fi>\n";
		return m_credit;
	}

}
