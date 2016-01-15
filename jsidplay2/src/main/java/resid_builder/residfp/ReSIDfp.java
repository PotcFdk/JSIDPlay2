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
package resid_builder.residfp;

import java.util.List;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDChip;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import resid_builder.ReSIDBase;

/**
 * Antti S. Lankila's resid-fp (distortion simulation)
 */
public class ReSIDfp extends ReSIDBase {

	/**
	 * FakeStereo mode uses two chips using the same base address. Write
	 * commands are routed two both SIDs, while read command can be configured
	 * to be processed by a specific SID chip.
	 * 
	 * @author ken
	 *
	 */
	public static class FakeStereo extends ReSIDfp {
		private final IEmulationSection emulationSection;
		private final int prevNum;
		private final List<ReSIDBase> sids;

		public FakeStereo(final EventScheduler context, final IConfig config, final int prevNum,
				final List<ReSIDBase> sids) {
			super(context);
			this.emulationSection = config.getEmulationSection();
			this.prevNum = prevNum;
			this.sids = sids;
		}

		@Override
		public byte read(int addr) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				return sids.get(prevNum).read(addr);
			}
			return super.read(addr);
		}

		@Override
		public byte readInternalRegister(int addr) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				return sids.get(prevNum).readInternalRegister(addr);
			}
			return super.readInternalRegister(addr);
		}

		@Override
		public void write(int addr, byte data) {
			super.write(addr, data);
			sids.get(prevNum).write(addr, data);
		}
	}

	private SID sidImpl;

	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 */
	public ReSIDfp(EventScheduler context) {
		super(context);
	}

	@Override
	protected SIDChip createSID() {
		return sidImpl = new SID();
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		boolean enable = emulation.isFilterEnable(sidNum);
		switch (sidImpl.getChipModel()) {
		case MOS6581:
			sidImpl.getFilter6581().enable(enable);
			break;
		case MOS8580:
			sidImpl.getFilter8580().enable(enable);
			break;
		default:
			throw new RuntimeException("Unknown SID chip model: " + sidImpl.getChipModel());
		}
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		IEmulationSection emulationSection = config.getEmulationSection();
		switch (sidImpl.getChipModel()) {
		case MOS6581:
			String filterName6581 = emulationSection.getFilterName(sidNum, Emulation.RESIDFP, ChipModel.MOS6581);
			final Filter6581 filter6581 = sidImpl.getFilter6581();
			if (filterName6581 == null) {
				filter6581.setCurveAndDistortionDefaults();
			} else {
				for (IFilterSection filter : config.getFilterSection()) {
					if (filter.getName().equals(filterName6581) && filter.isReSIDfpFilter6581()) {
						filter6581.setCurveProperties(filter.getBaseresistance(), filter.getOffset(),
								filter.getSteepness(), filter.getMinimumfetresistance());
						filter6581.setDistortionProperties(filter.getAttenuation(), filter.getNonlinearity(),
								filter.getResonanceFactor());
						sidImpl.set6581VoiceNonlinearity(filter.getVoiceNonlinearity());
						filter6581.setNonLinearity(filter.getVoiceNonlinearity());
						break;
					}
				}
			}
			break;
		case MOS8580:
			String filterName8580 = emulationSection.getFilterName(sidNum, Emulation.RESIDFP, ChipModel.MOS8580);
			final Filter8580 filter8580 = sidImpl.getFilter8580();
			if (filterName8580 == null) {
				filter8580.setCurveAndDistortionDefaults();
			} else {
				for (IFilterSection filter : config.getFilterSection()) {
					if (filter.getName().equals(filterName8580) && filter.isReSIDfpFilter8580()) {
						filter8580.setCurveProperties(filter.getK(), filter.getB(), 0, 0);
						filter8580.setDistortionProperties(0, 0, filter.getResonanceFactor());
						break;
					}
				}
			}
			break;
		default:
			throw new RuntimeException("Unknown SID chip model: " + sidImpl.getChipModel());
		}
	}

	@Override
	public byte readENV(int voiceNum) {
		return sidImpl.voice[voiceNum].envelope.readENV();
	}

	@Override
	public byte readOSC(int voiceNum) {
		return sidImpl.voice[voiceNum].wave.readOSC(sidImpl.getChipModel());
	}

	/**
	 * Credits string.
	 *
	 * @return String of credits.
	 */
	public static final String credits() {
		String credit = "MOS6581/8580 (SID) - Antti S. Lankila's resid-fp (distortion simulation):\n";
		credit += "\tCopyright (©) 1999-2004 Dag Lem <resid@nimrod.no>\n";
		credit += "\tCopyright (©) 2005-2011 Antti S. Lankila <alankila@bel.fi>\n";
		return credit;
	}

}
