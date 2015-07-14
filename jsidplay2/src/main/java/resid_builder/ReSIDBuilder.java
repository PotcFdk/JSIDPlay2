/**
 *             ReSID builder class for creating/controlling resids
 *             ---------------------------------------------------
 *  begin                : Wed Sep 5 2001
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

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import resid_builder.resid.ReSID;
import resid_builder.residfp.ReSIDfp;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

public class ReSIDBuilder extends SIDMixer implements SIDBuilder {

	public ReSIDBuilder(EventScheduler context, IConfig config,
			AudioConfig audioConfig, CPUClock cpuClock, AudioDriver audioDriver) {
		super(context, config, cpuClock, audioConfig, audioDriver);
	}

	/**
	 * Create a SID chip implementation and configure it, then start mixing.
	 */
	@Override
	public SIDEmu lock(SIDEmu oldSIDEmu, int sidNum, SidTune tune) {
		final ReSIDBase sid = getOrCreateSID(oldSIDEmu, tune, sidNum);
		IEmulationSection emulationSection = config.getEmulationSection();
		sid.setChipModel(ChipModel.getChipModel(emulationSection, tune, sidNum));
		sid.setClockFrequency(cpuClock.getCpuFrequency());
		sid.setFilter(config, sidNum);
		sid.setFilterEnable(emulationSection, sidNum);
		sid.input(emulationSection.isDigiBoosted8580() ? sid
				.getInputDigiBoost() : 0);
		add(sidNum, sid);
		return sid;
	}

	/**
	 * Stop mixing SID chip.
	 */
	@Override
	public void unlock(final SIDEmu sid) {
		remove(sid);
	}

	/**
	 * Create SID emulation of a specific emulation type or re-use already used
	 * SID chip, if implementation does not change.<BR>
	 * Note: The reason for re-using SID implementation is to preserve the
	 * current SID's internal state, when changing filters or chip model type.
	 * 
	 * @param oldSIDEmu
	 *            currently used SID chip
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * 
	 * @return new or re-used SID emulation of a specific emulation engine
	 */
	private ReSIDBase getOrCreateSID(SIDEmu oldSIDEmu, SidTune tune, int sidNum) {
		final IEmulationSection emulationSection = config.getEmulationSection();
		final Emulation emulation = Emulation.getEmulation(emulationSection,
				tune, sidNum);
		boolean fakeStereo = isFakeStereoSid(tune, sidNum);
		Class<? extends ReSIDBase> sidImplClass = getSIDImplClass(emulation,
				fakeStereo);
		if (oldSIDEmu != null && oldSIDEmu.getClass().equals(sidImplClass)) {
			// the implementing class does not change, re-use!
			return (ReSIDBase) oldSIDEmu;
		}
		return createSID(sidImplClass, sidNum);
	}

	/**
	 * Detect fake-stereo SID (second SID at the same address).
	 * 
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * @return fake-stereo SID has been detected
	 */
	private boolean isFakeStereoSid(SidTune tune, int sidNum) {
		int prevNum = sidNum > 0 ? sidNum - 1 : sidNum;
		IEmulationSection emulationSection = config.getEmulationSection();
		int prevAddres = SidTune.getSIDAddress(emulationSection, tune, prevNum);
		int baseAddress = SidTune.getSIDAddress(emulationSection, tune, sidNum);
		return sidNum > 0 && prevAddres == baseAddress;
	}

	/**
	 * Get SID chip implementation class.
	 * 
	 * @param emulation
	 *            wanted emulation type
	 * @param fakeStereo
	 *            fake-stereo mode (two SIDs at the same address)
	 * @return SID implementation class
	 */
	private Class<? extends ReSIDBase> getSIDImplClass(
			final Emulation emulation, final boolean fakeStereo) {
		switch (emulation) {
		case RESID:
			return fakeStereo ? ReSID.FakeStereo.class : ReSID.class;
		case RESIDFP:
			return fakeStereo ? ReSIDfp.FakeStereo.class : ReSIDfp.class;
		default:
			throw new RuntimeException("Unknown SID emulation: " + emulation);
		}
	}

	/**
	 * Create a new SID chip implemention.
	 * 
	 * @param sidImplCls
	 *            SID implementation class
	 * @param sidNum
	 *            current SID number
	 * @return new SID chip
	 */
	private ReSIDBase createSID(final Class<? extends ReSIDBase> sidImplCls,
			int sidNum) {
		if (ReSID.class.equals(sidImplCls)) {
			return new ReSID(context);
		} else if (ReSIDfp.class.equals(sidImplCls)) {
			return new ReSIDfp(context);
		} else if (ReSID.FakeStereo.class.equals(sidImplCls)) {
			return new ReSID.FakeStereo(context, config, sidNum - 1, sids);
		} else if (ReSIDfp.FakeStereo.class.equals(sidImplCls)) {
			return new ReSIDfp.FakeStereo(context, config, sidNum - 1, sids);
		} else {
			throw new RuntimeException("Unknown SID impl.: " + sidImplCls);
		}
	}

}
