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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import resid_builder.resid.ISIDDefs.ChipModel;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

public class ReSIDBuilder extends SIDBuilder {
	/** Current audio configuration */
	final AudioConfig audioConfig;

	/** Current output driver */
	protected AudioDriver output;
	
	/** List of SID instances */
	protected List<ReSID> sids = new ArrayList<ReSID>();

	/** Mixing algorithm */
	private final MixerEvent mixerEvent = new MixerEvent();
	
	/** C64 system frequency */
	private final double systemFrequency;
	
	public ReSIDBuilder(AudioConfig audioConfig, double systemFrequency) {
		this.audioConfig = audioConfig;
		this.systemFrequency = systemFrequency;
	}
	
	protected class MixerEvent extends Event {
		/** Random source for triangular dithering */	
		private final Random RANDOM = new Random();
		/** State of HP-TPDF. */
		private int oldRandomValue;

		private final int[] volume = new int[] { 1024, 1024 };
		private EventScheduler context;
		
		protected void setVolume(int i, float v) {
			volume[i] = (int) (v * 1024f);
		}

		/**
		 * Triangularly shaped noise source for audio applications.
		 * Output of this PRNG is between ]-1, 1[.
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
			
			/* this clocks the SID to the present moment, if it isn't already. */
			chip1.clock();
			final int[] buf1 = chip1.getBuffer();
			int[] buf2 = null;
			if (chip2 != null) {
				chip2.clock();
				buf2 = chip2.getBuffer();
			}
			
			/* extract buffer info now that the SID is updated.
			 * clock() may update bufferpos. */
			final int samples = chip1.getPosition();
			/* If chip2 exists, its bufferpos is expected to be identical to chip1's. */
			
			final ByteBuffer soundBuffer = output.buffer();
			for (int i = 0; i < samples; i ++) {
				int dither = triangularDithering();
				int value = (buf1[i] * volume[0] + dither) >> 10;
				if (value > 32767) {
					value = 32767;
				}
				if (value < -32768) {
					value = -32768;
				}
				soundBuffer.putShort((short) value);

				if (buf2 != null) {
					value = (buf2[i] * volume[1] + dither) >> 10;
					if (value > 32767) {
						value = 32767;
					}
					if (value < -32768) {
						value = -32768;
					}
					
					soundBuffer.putShort((short) value);
				}
				
				if (soundBuffer.remaining() == 0) {
					output.write();
					soundBuffer.clear();
				}
			}

			chip1.setPosition(0);
			if (chip2 != null) {
				chip2.setPosition(0);
			}
			
			context.schedule(this, 10000);
		}

		public void setContext(EventScheduler env) {
			this.context = env;
		}
	}

	/**
	 * Make a new SID of right type
	 */
	@Override
	public SIDEmu lock(final EventScheduler env, final ChipModel model) {
		final ReSID sid = new ReSID(env, mixerEvent);
		sid.model(model);
		sid.sampling(systemFrequency, audioConfig.getFrameRate(), audioConfig.getSamplingMethod());
		sids.add(sid);
		return sid;
	}

	/**
	 * No implementation, just builder API compat.
	 */
	@Override
	public void unlock(final SIDEmu sid) {
		sids.remove(sid);
	}

	public void setSIDVolume(int i, float volume) {
		mixerEvent.setVolume(i, volume);
	}

	public void setOutput(AudioDriver driver) {
		output = driver;
	}
}
