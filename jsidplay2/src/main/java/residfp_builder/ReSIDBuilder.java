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
package residfp_builder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.sidtune.SidTune;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IConfig;

public class ReSIDBuilder extends SIDBuilder {
	protected class MixerEvent extends Event {
		/** Random source for triangular dithering */
		private final Random RANDOM = new Random();
		/** State of HP-TPDF. */
		private int oldRandomValue;

		private final int[] volume = new int[] { 1024, 1024, 1024 };
		private final float[] balance = new float[] { 0, 1, 1 };
		private EventScheduler context;

		protected void setVolume(int i, float v) {
			this.volume[i] = (int) (v * 1024f);
		}

		public void setBalance(int i, float balance) {
			this.balance[i] = balance;
		}

		/**
		 * Triangularly shaped noise source for audio applications. Output of
		 * this PRNG is between ]-1, 1[.
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

		@Override
		public void event() throws InterruptedException {
			final ReSID chip1 = sids.get(0);
			final ReSID chip2 = sids.size() >= 2 ? sids.get(1) : null;
			final ReSID chip3 = sids.size() >= 3 ? sids.get(2) : null;

			/* this clocks the SID to the present moment, if it isn't already. */
			chip1.clock();
			final float[] buf1 = chip1.buffer;
			float[] buf2 = null;
			if (chip2 != null) {
				chip2.clock();
				buf2 = chip2.buffer;
			}
			float[] buf3 = null;
			if (chip3 != null) {
				chip3.clock();
				buf3 = chip3.buffer;
			}

			/*
			 * extract buffer info now that the SID is updated. clock() may
			 * update bufferpos.
			 */
			final int samples = chip1.bufferpos;
			/*
			 * If chip2 exists, its bufferpos is expected to be identical to
			 * chip1's.
			 */
			float value;
			final ByteBuffer soundBuffer = driver.buffer();
			for (int i = 0; i < samples; i++) {
				int dither = triangularDithering();

				value = (buf1[i]) * volume[0] * (1 - this.balance[0]);
				if (buf2 != null) {
					value += buf2[i] * volume[1] * (1 - this.balance[1]);
				}
				if (buf3 != null) {
					value += buf3[i]
							* (int) (volume[2] * (1 - this.balance[2]));
				}
				value *= 32768f;
				value += dither;
				value = Math.round(value) >> 10;

				if (value > 32767) {
					value = 32767;
				}
				if (value < -32768) {
					value = -32768;
				}
				soundBuffer.putShort((short) value);

				if (buf2 != null) {
					value = buf1[i] * volume[0] * this.balance[0];
					value += buf2[i] * volume[1] * this.balance[1];
					if (buf3 != null) {
						value += buf3[i] * volume[2] * this.balance[2];
					}
					value *= 32768f;
					value += dither;
					value = Math.round(value) >> 10;

					if (value > 32767) {
						value = 32767;
					}
					if (value < -32768) {
						value = -32768;
					}

					soundBuffer.putShort((short) value);
				}

				if (soundBuffer.remaining() == 0) {
					driver.write();
					soundBuffer.clear();
				}
			}

			chip1.bufferpos = 0;
			if (chip2 != null) {
				chip2.bufferpos = 0;
			}
			if (chip3 != null) {
				chip3.bufferpos = 0;
			}

			context.schedule(this, 10000);
		}

		public void setContext(EventScheduler env) {
			this.context = env;
		}
	}

	/** Current audio configuration */
	private final AudioConfig audioConfig;

	/** C64 system frequency */
	private final CPUClock cpuClock;

	/** output driver */
	protected AudioDriver driver, realDriver;

	/** List of SID instances */
	protected List<ReSID> sids = new ArrayList<ReSID>();

	/** Mixing algorithm */
	private final MixerEvent mixerEvent = new MixerEvent();

	public ReSIDBuilder(IConfig config, AudioConfig audioConfig,
			CPUClock cpuClock, AudioDriver audio, SidTune tune) {
		this.audioConfig = audioConfig;
		this.cpuClock = cpuClock;
		this.driver = audio;
		setMixerVolume(0, config.getAudio().getLeftVolume());
		setMixerVolume(1, config.getAudio().getRightVolume());
		setBalance(0, config.getAudio().getLeftBalance());
		setBalance(1, config.getAudio().getRightBalance());
		switchToNullDriver(tune);
	}

	@Override
	public void start() {
		switchToAudioDriver();
	}

	@Override
	public SIDEmu lock(final EventScheduler evt, SIDEmu device, ChipModel model) {
		if (device == null) {
			device = lock(evt, model);
		} else {
			device.setChipModel(model);
		}
		return device;
	}

	/**
	 * No implementation, just builder API compat.
	 */
	@Override
	public void unlock(final SIDEmu sid) {
		sids.remove(sid);
	}

	@Override
	public void setMixerVolume(int i, float volumeInDB) {
		mixerEvent.setVolume(i, (float) Math.pow(10, volumeInDB / 10));
	}

	@Override
	public void setBalance(int i, float balance) {
		mixerEvent.setBalance(i, balance);
	}

	@Override
	public int getNumDevices() {
		return sids.size();
	}

	/**
	 * Make a new SID of right type
	 */
	private SIDEmu lock(final EventScheduler env, final ChipModel model) {
		final ReSID sid = new ReSID(env, mixerEvent);
		sid.setChipModel(model);
		sid.setSampling(cpuClock.getCpuFrequency(), audioConfig.getFrameRate(),
				audioConfig.getSamplingMethod());
		sids.add(sid);
		return sid;
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

}
