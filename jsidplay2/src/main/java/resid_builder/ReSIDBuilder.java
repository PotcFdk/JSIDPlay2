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
import libsidplay.common.Emulation;
import libsidplay.common.EventScheduler;
import libsidplay.common.ReSIDBase;
import libsidplay.common.ReSIDBuilderBase;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

public class ReSIDBuilder extends ReSIDBuilderBase {

	public ReSIDBuilder(AudioConfig audioConfig, CPUClock cpuClock,
			AudioDriver audio, SidTune tune) {
		super(audioConfig, cpuClock, audio, tune);
	}

	/**
	 * Create SID emulation of a specific emulation engine type.<BR>
	 * Note: FakeStereo mode uses two chips using the same base address. Write
	 * commands are routed two both SIDs.
	 * 
	 * @return SID emulation of a specific emulation engine
	 */
	@Override
	protected ReSIDBase createSIDEmu(EventScheduler context, IConfig config,
			SidTune tune, int sidNum) {
		final IEmulationSection emulationSection = config.getEmulation();
		final Emulation emulation = Emulation.getEmulation(emulationSection,
				tune, sidNum);

		boolean isStereo = AudioConfig.isSIDUsed(emulationSection, tune, 1);
		int stereoAddress = AudioConfig
				.getSIDAddress(emulationSection, tune, 1);
		if (isStereo && sidNum == 1
				&& Integer.valueOf(0xd400).equals(stereoAddress)) {
			/** Stereo SID at 0xd400 hack */
			final ReSIDBase firstSid = sids.get(0);
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
				return new residfp_builder.ReSID(context, config.getAudio()
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
		/** normal case **/
		if (emulation.equals(Emulation.RESID)) {
			return new ReSID(context, config.getAudio().getBufferSize());
		} else if (emulation.equals(Emulation.RESIDFP)) {
			return new residfp_builder.ReSID(context, config.getAudio()
					.getBufferSize());
		}
		throw new RuntimeException("Cannot create SID emulation engine: "
				+ emulation);
	}

}
