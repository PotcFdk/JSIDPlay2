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
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

public class ReSIDBuilder implements SIDBuilder {

	/**
	 * Configuration
	 */
	private IConfig config;

	/**
	 * Current audio configuration
	 */
	private final AudioConfig audioConfig;

	/**
	 * C64 system frequency
	 */
	private final CPUClock cpuClock;

	/**
	 * Mixing algorithm
	 */
	protected final Mixer mixer;

	public ReSIDBuilder(EventScheduler context, IConfig config,
			AudioConfig audioConfig, CPUClock cpuClock, AudioDriver audioDriver) {
		this.config = config;
		this.audioConfig = audioConfig;
		this.cpuClock = cpuClock;
		this.mixer = new Mixer(context, audioDriver);
	}

	@Override
	public SIDEmu lock(EventScheduler context, IConfig config,
			SIDEmu oldSIDEmu, int sidNum, SidTune tune) {
		final ReSIDBase sid = getOrCreateSID(context, oldSIDEmu, tune, sidNum);
		sid.setChipModel(ChipModel.getChipModel(config.getEmulation(), tune,
				sidNum));
		sid.setSampling(cpuClock.getCpuFrequency(), audioConfig.getFrameRate(),
				audioConfig.getSamplingMethod());
		sid.setFilter(config, sidNum);
		sid.setFilterEnable(config.getEmulation(), sidNum);
		sid.input(config.getEmulation().isDigiBoosted8580() ? sid
				.getInputDigiBoost() : 0);
		mixer.add(sidNum, sid);
		return sid;
	}

	/**
	 * No implementation, just builder API compat.
	 */
	@Override
	public void unlock(final SIDEmu sid) {
		mixer.remove(sid);
	}

	/**
	 * Reset.
	 */
	@Override
	public void reset() {
		mixer.reset();
	}

	/**
	 * Timer start time has been reached, start mixing.
	 */
	@Override
	public void start() {
		mixer.start(audioConfig, config.getAudio());
	}

	/**
	 * How many SID chips are in the mix?
	 */
	@Override
	public int getNumDevices() {
		return mixer.getNumDevices();
	}

	/**
	 * Volume of the SID chip.<BR>
	 * 0(-6db)..12(+6db)
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            audio configuration
	 */
	@Override
	public void setVolume(int sidNum, IAudioSection audio) {
		mixer.setVolume(sidNum, audio);
	}

	/**
	 * Set left/right speaker balance for each SID.<BR>
	 * 0(left speaker)..0.5(centered)..1(right speaker)
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            audio configuration
	 */
	@Override
	public void setBalance(int sidNum, IAudioSection audio) {
		mixer.setBalance(sidNum, audio);
	}

	/**
	 * Create SID emulation of a specific emulation type or re-use already used
	 * SID chip, if implementation does not change.<BR>
	 * Note: The reason for re-using SID implementation is to preserve the
	 * current SID's internal state, when changing filters ot chip model type.
	 * 
	 * @param context
	 *            System event context
	 * @param oldSIDEmu
	 *            currently used SID chip
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * 
	 * @return new or re-used SID emulation of a specific emulation engine
	 */
	private ReSIDBase getOrCreateSID(EventScheduler context, SIDEmu oldSIDEmu,
			SidTune tune, int sidNum) {
		final IEmulationSection emulationSection = config.getEmulation();
		final Emulation emulation = Emulation.getEmulation(emulationSection,
				tune, sidNum);
		boolean fakeStereo = isFakeStereo(tune, sidNum, emulationSection);
		Class<? extends ReSIDBase> sidImlClass = getSIDImplClass(emulation,
				fakeStereo);
		if (oldSIDEmu != null && oldSIDEmu.getClass().equals(sidImlClass)) {
			// the implementing class does not change, re-use!
			return (ReSIDBase) oldSIDEmu;
		}
		return createSID(context, sidImlClass, sidNum);
	}

	/**
	 * Detect fake-stereo mode (two SIDs at the same address).
	 * 
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * @param emulationSection
	 *            configuration
	 * @return fake-stereo mode has been detected
	 */
	private boolean isFakeStereo(SidTune tune, int sidNum,
			final IEmulationSection emulationSection) {
		int prevNum = sidNum > 0 ? sidNum - 1 : sidNum;
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
	 * @param context
	 *            System event context.
	 * @param sidImplCls
	 *            SID implementation class
	 * @param sidNum
	 *            current SID number
	 * @return new SID chip
	 */
	private ReSIDBase createSID(final EventScheduler context,
			final Class<? extends ReSIDBase> sidImplCls, int sidNum) {
		if (ReSID.class.equals(sidImplCls)) {
			return new ReSID(context, config.getAudio().getBufferSize());
		} else if (ReSIDfp.class.equals(sidImplCls)) {
			return new ReSIDfp(context, config.getAudio().getBufferSize());
		} else if (ReSID.FakeStereo.class.equals(sidImplCls)) {
			// ReSID fake-stereo mode
			final int prevNum = sidNum - 1;
			final ReSIDBase prevSID = mixer.get(prevNum);
			return new ReSID.FakeStereo(context, config, prevNum, prevSID);
		} else if (ReSIDfp.FakeStereo.class.equals(sidImplCls)) {
			// ReSIDfp fake-stereo mode
			final int prevNum = sidNum - 1;
			final ReSIDBase prevSID = mixer.get(prevNum);
			return new ReSIDfp.FakeStereo(context, config, prevNum, prevSID);
		} else {
			throw new RuntimeException("Unknown SID impl.: " + sidImplCls);
		}
	}

}
