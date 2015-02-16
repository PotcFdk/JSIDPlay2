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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.sidtune.SidTune;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

public class ReSIDBuilder implements SIDBuilder {

	public class MixerEvent extends Event {
		/** Random source for triangular dithering */
		private final Random RANDOM = new Random();
		/** State of HP-TPDF. */
		private int oldRandomValue;

		private final float[] volume = new float[] { 1024f, 1024f, 1024f };
		private final float[] positionL = new float[] { 1, 0, .5f };
		private final float[] positionR = new float[] { 0, 1, .5f };

		private transient final float[] balancedVolumeL = new float[] { 1024,
				0, 512 };
		private transient final float[] balancedVolumeR = new float[] { 0,
				1024, 512 };

		private void setVolume(int i, float v) {
			this.volume[i] = v * 1024;
			updateFactor();
		}

		private void setBalance(int i, float balance) {
			this.positionL[i] = 1 - balance;
			this.positionR[i] = balance;
			updateFactor();
		}

		private void updateFactor() {
			for (int i = 0; i < balancedVolumeL.length; i++) {
				balancedVolumeL[i] = volume[i] * positionL[i];
				balancedVolumeR[i] = volume[i] * positionR[i];
			}
		}

		/**
		 * Triangularly shaped noise source for audio applications. Output of
		 * this PRNG is between ]-1, 1[ * 1024.
		 * 
		 * @return triangular noise sample
		 */
		private int triangularDithering() {
			int prevValue = oldRandomValue;
			oldRandomValue = RANDOM.nextInt() & 0x3ff;
			return oldRandomValue - prevValue;
		}

		protected MixerEvent() {
			super("Mixer");
		}

		/**
		 * Note: The assumption, that after clocking two chips their buffer
		 * positions are equal is false! Under some circumstance one chip can be
		 * one sample further than the other. Therefore we have to handle
		 * overflowing sample data to prevent crackling noises.
		 */
		@Override
		public void event() throws InterruptedException {
			synchronized (sids) {
				int numSids = 0;
				int samples = 0;
				for (ReSIDBase sid : sids) {
					// clock SID to the present moment
					sid.clock();
					buffers[numSids++] = sid.buffer;
					// determine amount of samples produced (cut-off overflows)
					samples = samples > 0 ? Math.min(samples, sid.bufferpos)
							: sid.bufferpos;
					sid.bufferpos = 0;
				}
				// output sample data
				for (int sampleIdx = 0; sampleIdx < samples; sampleIdx++) {
					int dither = triangularDithering();

					putSample(sampleIdx, channels > 1 ? balancedVolumeL
							: volume, dither);
					if (channels > 1) {
						putSample(sampleIdx, balancedVolumeR, dither);
					}
					if (soundBuffer.remaining() == 0) {
						driver.write();
						soundBuffer.clear();
					}
				}
			}
			context.schedule(this, 10000);
		}

		private final void putSample(int sampleIdx, float[] balancedVolume,
				int dither) {
			int value = 0;
			for (int i = 0; i < sids.size(); i++) {
				value += buffers[i][sampleIdx] * balancedVolume[i];
			}
			value = value + dither >> 10;

			if (value > 32767) {
				value = 32767;
			}
			if (value < -32768) {
				value = -32768;
			}
			soundBuffer.putShort((short) value);
		}

	}

	/** Configuration **/
	private IConfig config;

	/** Current audio configuration */
	private final AudioConfig audioConfig;

	/** C64 system frequency */
	private final CPUClock cpuClock;

	private int[][] buffers;
	private int channels;
	private ByteBuffer soundBuffer;

	/** output driver */
	protected AudioDriver driver, realDriver;

	/** List of SID instances */
	protected List<ReSIDBase> sids = Collections
			.synchronizedList(new ArrayList<ReSIDBase>());

	/** Mixing algorithm */
	protected final MixerEvent mixerEvent = new MixerEvent();

	private EventScheduler context;

	public ReSIDBuilder(IConfig config, AudioConfig audioConfig,
			CPUClock cpuClock, AudioDriver audio, SidTune tune) {
		this.config = config;
		this.audioConfig = audioConfig;
		this.cpuClock = cpuClock;
		this.driver = audio;
		switchToNullDriver(tune);
	}

	@Override
	public void start(final EventScheduler context) {
		this.context = context;
		/*
		 * No matter how many chips are in use, mixerEvent is singleton with
		 * respect to them. Only one will be scheduled. This is a bit dirty,
		 * though.
		 */
		context.cancel(mixerEvent);
		synchronized (sids) {
			buffers = new int[sids.size()][];
			channels = audioConfig.getChannels();
			soundBuffer = realDriver.buffer();
			for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
				setVolume(sidNum, config.getAudio());
				setBalance(sidNum, config.getAudio());
			}
		}
		context.schedule(mixerEvent, 0, Event.Phase.PHI2);
		switchToAudioDriver();
	}

	@Override
	public int getNumDevices() {
		return sids.size();
	}

	@Override
	public SIDEmu lock(EventScheduler context, IConfig config, SIDEmu device,
			int sidNum, SidTune tune) {
		synchronized (sids) {
			final ReSIDBase sid = createSIDEmu(context, tune, sidNum);
			sid.setChipModel(ChipModel.getChipModel(config.getEmulation(),
					tune, sidNum));
			sid.setSampling(cpuClock.getCpuFrequency(),
					audioConfig.getFrameRate(), audioConfig.getSamplingMethod());
			sid.setFilter(config, sidNum);
			sid.setFilterEnable(config.getEmulation(), sidNum);
			sid.input(config.getEmulation().isDigiBoosted8580() ? sid
					.getInputDigiBoost() : 0);
			if (sidNum < sids.size()) {
				sids.set(sidNum, sid);
			} else {
				sids.add(sid);
			}
			return sid;
		}
	}

	/**
	 * No implementation, just builder API compat.
	 */
	@Override
	public void unlock(final SIDEmu sid) {
		sids.remove(sid);
	}

	@Override
	public void setVolume(int num, IAudioSection audio) {
		float volumeInDB;
		switch (num) {
		case 0:
			volumeInDB = audio.getMainVolume();
			break;
		case 1:
			volumeInDB = audio.getSecondVolume();
			break;
		case 2:
			volumeInDB = audio.getThirdVolume();
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		mixerEvent.setVolume(num, (float) Math.pow(10, volumeInDB / 10));
	}

	@Override
	public void setBalance(int num, IAudioSection audio) {
		float balance;
		switch (num) {
		case 0:
			balance = audio.getMainBalance();
			break;
		case 1:
			balance = audio.getSecondBalance();
			break;
		case 2:
			balance = audio.getThirdBalance();
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		mixerEvent.setBalance(num, balance);
	}

	/**
	 * Before the timer start time is being reached, use NULL driver to shorten
	 * the duration to wait for the user.
	 * 
	 * @param tune
	 */
	private void switchToNullDriver(SidTune tune) {
		this.realDriver = driver;
		this.driver = Audio.NONE.getAudioDriver();
		try {
			Audio.NONE.getAudioDriver().open(audioConfig, tune);
		} catch (LineUnavailableException | UnsupportedAudioFileException
				| IOException e) {
		}
	}

	/**
	 * When the start time is being reached, switch to the real audio output.
	 */
	private void switchToAudioDriver() {
		driver = realDriver;
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
			/** Stereo SID at 0xd400 hack */
			synchronized (sids) {
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
		}
		/** normal case **/
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
