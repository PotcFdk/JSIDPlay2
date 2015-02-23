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

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDChip;
import resid_builder.residfp.Filter6581;
import resid_builder.residfp.Filter8580;
import resid_builder.residfp.SID;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;

/**
 * Antti S. Lankila's resid-fp (distortion simulation)
 */
public class ReSIDfp extends ReSIDBase {

	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 * @param mixerEvent
	 *            {@link Mixer} to use.
	 */
	public ReSIDfp(EventScheduler context, final int bufferSize) {
		super(context, bufferSize);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		SID sidImpl = (SID) sid;
		final Filter6581 filter6581 = sidImpl.getFilter6581();
		final Filter8580 filter8580 = sidImpl.getFilter8580();

		String filterName6581 = config.getEmulation().getFilterName(sidNum,
				Emulation.RESIDFP, ChipModel.MOS6581);
		String filterName8580 = config.getEmulation().getFilterName(sidNum,
				Emulation.RESIDFP, ChipModel.MOS8580);
		if (filterName6581 == null) {
			filter6581.setCurveAndDistortionDefaults();
		}
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
				sidImpl.set6581VoiceNonlinearity(filter.getVoiceNonlinearity());
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
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		boolean enable = emulation.isFilterEnable(sidNum);
		SID sidImpl = (SID) sid;
		sidImpl.getFilter6581().enable(enable);
		sidImpl.getFilter8580().enable(enable);
	}

	/**
	 * Credits string.
	 *
	 * @return String of credits.
	 */
	public static final String credits() {
		String m_credit = "MOS6581/8580 (SID) - Antti S. Lankila's resid-fp (distortion simulation):\n";
		m_credit += "\tCopyright (©) 1999-2004 Dag Lem <resid@nimrod.no>\n";
		m_credit += "\tCopyright (©) 2005-2011 Antti S. Lankila <alankila@bel.fi>\n";
		return m_credit;
	}

	@Override
	protected SIDChip createSID() {
		return new SID();
	}
}
