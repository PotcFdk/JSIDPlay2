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
import resid_builder.resid.Filter6581;
import resid_builder.resid.Filter8580;
import resid_builder.resid.SID;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;

/**
 * Dag Lem's resid 1.0 beta
 */
public class ReSID extends ReSIDBase {

	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 * @param mixerEvent
	 *            {@link Mixer} to use.
	 */
	public ReSID(EventScheduler context, final int bufferSize) {
		super(context, bufferSize);
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		boolean enable = emulation.isFilterEnable(sidNum);
		SID sidImpl = (SID) sid;
		sidImpl.getFilter6581().enable(enable);
		sidImpl.getFilter8580().enable(enable);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		SID sidImpl = (SID) sid;
		final Filter6581 filter6581 = sidImpl.getFilter6581();
		final Filter8580 filter8580 = sidImpl.getFilter8580();

		String filterName6581 = config.getEmulation().getFilterName(sidNum,
				Emulation.RESID, ChipModel.MOS6581);
		String filterName8580 = config.getEmulation().getFilterName(sidNum,
				Emulation.RESID, ChipModel.MOS8580);
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
	public byte read(int addr) {
		// correction for sid_detection.prg
		lastTime--;
		return super.read(addr);
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

	@Override
	protected SIDChip createSID() {
		return new SID();
	}

}