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
	 **/
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
	public SIDEmu lock(EventScheduler context, IConfig config, SIDEmu device,
			int sidNum, SidTune tune) {
		final ReSIDBase sid = createSIDEmu(context, tune, sidNum);
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

	@Override
	public void reset() {
		mixer.reset();
	}

	/**
	 * Timer start time has been reached, start mixing.
	 */
	@Override
	public void start() {
		/*
		 * No matter how many chips are in use, mixerEvent is singleton with
		 * respect to them. Only one will be scheduled. This is a bit dirty,
		 * though.
		 */
		mixer.start(audioConfig, config.getAudio());
	}

	@Override
	public int getNumDevices() {
		return mixer.getNumDevices();
	}

	@Override
	public void setVolume(int num, IAudioSection audio) {
		mixer.setVolume(num, audio);
	}

	@Override
	public void setBalance(int num, IAudioSection audio) {
		mixer.setBalance(num, audio);
	}

	/**
	 * Create SID emulation of a specific emulation engine type.<BR>
	 * Note: FakeStereo mode uses two chips using the same base address. Write
	 * commands are routed two both SIDs.
	 * 
	 * @return SID emulation of a specific emulation engine
	 */
	protected ReSIDBase createSIDEmu(EventScheduler context, SidTune tune,
			int sidNum) {
		final IEmulationSection emulationSection = config.getEmulation();
		final Emulation emulation = Emulation.getEmulation(emulationSection,
				tune, sidNum);

		boolean isStereo = AudioConfig.isSIDUsed(emulationSection, tune, 1);
		int address = AudioConfig.getSIDAddress(emulationSection, tune, 0);
		int stereoAddress = AudioConfig
				.getSIDAddress(emulationSection, tune, 1);
		if (isStereo && sidNum == 1 && address == stereoAddress) {
			// Stereo SID at 0xd400 hack
			final ReSIDBase firstSid = mixer.get(0);
			if (emulation.equals(Emulation.RESID)) {
				return new ReSID(context, config.getAudio().getBufferSize()) {
					@Override
					public byte read(int addr) {
						if (emulationSection.getSidNumToRead() > 0) {
							return firstSid.read(addr);
						}
						return super.read(addr);
					}

					@Override
					public byte readInternalRegister(int addr) {
						if (emulationSection.getSidNumToRead() > 0) {
							return firstSid.readInternalRegister(addr);
						}
						return super.readInternalRegister(addr);
					}

					@Override
					public void write(int addr, byte data) {
						super.write(addr, data);
						firstSid.write(addr, data);
					}
				};
			} else if (emulation.equals(Emulation.RESIDFP)) {
				return new resid_builder.ReSIDfp(context, config.getAudio()
						.getBufferSize()) {
					@Override
					public byte read(int addr) {
						if (emulationSection.getSidNumToRead() > 0) {
							return firstSid.read(addr);
						}
						return super.read(addr);
					}

					@Override
					public byte readInternalRegister(int addr) {
						if (emulationSection.getSidNumToRead() > 0) {
							return firstSid.readInternalRegister(addr);
						}
						return super.readInternalRegister(addr);
					}

					@Override
					public void write(int addr, byte data) {
						super.write(addr, data);
						firstSid.write(addr, data);
					}
				};
			}
		}
		// normal case
		if (emulation.equals(Emulation.RESID)) {
			return new ReSID(context, config.getAudio().getBufferSize());
		} else if (emulation.equals(Emulation.RESIDFP)) {
			return new resid_builder.ReSIDfp(context, config.getAudio()
					.getBufferSize());
		}
		throw new RuntimeException("Cannot create SID emulation engine: "
				+ emulation);
	}

}
