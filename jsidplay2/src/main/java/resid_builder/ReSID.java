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

import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.ReSIDBase;
import libsidplay.common.ReSIDBuilderBase.MixerEvent;
import libsidplay.common.SamplingMethod;
import resid_builder.resid.Filter6581;
import resid_builder.resid.Filter8580;
import resid_builder.resid.SID;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;

/**
 * ReSID emulation.
 */
public class ReSID extends ReSIDBase {
	private static final Logger RESID = Logger.getLogger(ReSID.class.getName());

	private final SID sid = new SID();

	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 * @param mixerEvent
	 *            {@link MixerEvent} to use.
	 */
	public ReSID(EventScheduler context, final int bufferSize) {
		super(context, bufferSize);
		reset((byte) 0);
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		sid.reset();
		sid.write(0x18, volume);
	}

	@Override
	public byte read(int addr) {
		addr &= 0x1f;
		// correction for sid_detection.prg
		lastTime--;
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
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		boolean enable;
		switch (sidNum) {
		case 0:
			enable = emulation.isFilter();
			break;
		case 1:
			enable = emulation.isStereoFilter();
			break;
		case 2:
			enable = emulation.isThirdSIDFilter();
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		sid.getFilter6581().enable(enable);
		sid.getFilter8580().enable(enable);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		final Filter6581 filter6581 = sid.getFilter6581();
		final Filter8580 filter8580 = sid.getFilter8580();

		String filterName6581 = null;
		String filterName8580 = null;
		switch (sidNum) {
		case 0:
			filterName6581 = config.getEmulation().getFilter6581();
			filterName8580 = config.getEmulation().getFilter8580();
			break;

		case 1:
			filterName6581 = config.getEmulation().getStereoFilter6581();
			filterName8580 = config.getEmulation().getStereoFilter8580();
			break;

		case 2:
			filterName6581 = config.getEmulation().getThirdSIDFilter6581();
			filterName8580 = config.getEmulation().getThirdSIDFilter8580();
			break;

		default:
			break;
		}
		for (IFilterSection filter : config.getFilter()) {
			if (filter.getName().equals(filterName6581)
					&& filter.isReSIDFilter6581()) {
				filter6581.setFilterCurve(filter.getFilter6581CurvePosition());
			} else if (filter.getName().equals(filterName8580)
					&& filter.isReSIDFilter8580()) {
				filter8580.setFilterCurve(filter.getFilter8580CurvePosition());
			}
		}
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
		sid.mute(num, mute);
	}

	/**
	 * Sets the SID sampling parameters.
	 *
	 * @param systemClock
	 *            System clock to use for the SID.
	 * @param freq
	 *            Frequency to use for the SID.
	 * @param method
	 *            {@link SamplingMethod} to use for the SID.
	 */
	public void setSampling(final double systemClock, final float freq,
			final SamplingMethod method) {
		sid.setSamplingParameters(systemClock, method, freq, 20000);
	}

	/**
	 * Set the emulated SID model
	 * 
	 * @param model
	 *            The emulated SID chip model to use.
	 */
	public void setChipModel(final ChipModel model) {
		sid.setChipModel(model);
	}

	@Override
	public void input(int input) {
		sid.input(input);
	}

	/**
	 * Credits string.
	 *
	 * @return String of credits.
	 */
	public static final String credits() {
		String m_credit = "MOS6581/8580 (SID) - Dag Lem's resid 1.0 beta:\n";
		m_credit += "\tCopyright (©) 1999-2012 Dag Lem <resid@nimrod.no>\n";
		m_credit += "\tCopyright (©) 2012 Java version by Antti S. Lankila <alankila@bel.fi>\n";
		return m_credit;
	}

	// Getters and setters.

	/**
	 * Gets the {@link SID} instance being used.
	 *
	 * @return The {@link SID} instance being used.
	 */
	public SID sid() {
		return sid;
	}

	@Override
	public ChipModel getChipModel() {
		return sid.getChipModel();
	}

	public int getInputDigiBoost() {
		return sid.getInputDigiBoost();
	}
}