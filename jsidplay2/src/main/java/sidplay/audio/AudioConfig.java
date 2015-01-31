/**
 *                                  description
 *                                  -----------
 *  begin                : Sat Jul 8 2000
 *  copyright            : (C) 2000 by Simon White
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
package sidplay.audio;

import libsidplay.common.SamplingMethod;
import libsidplay.sidtune.SidTune;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

public class AudioConfig {
	protected int frameRate = 48000;
	protected int channels = 1;
	protected int bufferFrames = 4096;
	private SamplingMethod samplingMethod;
	private int deviceIdx;

	/**
	 * This instance represents the requested audio configuration
	 * 
	 * @param frameRate
	 *            The desired audio framerate.
	 * @param channels
	 *            The number of audio channels to use.
	 */
	protected AudioConfig(int frameRate, int channels,
			SamplingMethod samplingMethod, int deviceIdx) {
		this.frameRate = frameRate;
		this.channels = channels;
		this.samplingMethod = samplingMethod;
		this.deviceIdx = deviceIdx;
	}

	/**
	 * Return a detached AudioConfig instance corresponding to current
	 * parameters.
	 * 
	 * @param channels
	 *            The number of audio channels to use.
	 * @return AudioConfig for current specification
	 */
	public static AudioConfig getInstance(IAudioSection audio, int channels) {
		return new AudioConfig(audio.getFrequency(), channels,
				audio.getSampling(), audio.getDevice());
	}

	public static AudioConfig getInstance(IConfig config, SidTune tune) {
		IEmulationSection emulation = config.getEmulation();
		return new AudioConfig(config.getAudio().getFrequency(), isSIDUsed(
				emulation, tune, 1) ? 2 : 1, config.getAudio().getSampling(),
				config.getAudio().getDevice());
	}

	/**
	 * Is SID used of specified SID number?
	 * <OL>
	 * <LI>0 - first SID is always used
	 * <LI>1 - second SID is only used for stereo tunes
	 * <LI>2 - third SID is used for triple SID tunes
	 * </OL>
	 */
	public static boolean isSIDUsed(IEmulationSection emulation, SidTune tune,
			int sidNum) {
		return getSIDAddress(emulation, tune, sidNum) != 0;
	}

	/**
	 * Get SID address of specified SID number
	 * <OL>
	 * <LI>0xd400 - always used for first SID
	 * <LI>forced SID base - configured value for forced stereo output
	 * <LI>tune SID base - SID base detected by tune information
	 * <LI>0 - SID is not used
	 * </OL>
	 */
	public static int getSIDAddress(IEmulationSection emulation, SidTune tune,
			int sidNum) {
		boolean forcedStereoTune;
		int forcedSidBase;
		int tuneChipBase;
		switch (sidNum) {
		case 0:
			return 0xd400;
		case 1:
			forcedStereoTune = emulation.isForceStereoTune();
			forcedSidBase = emulation.getDualSidBase();
			tuneChipBase = tune != null ? tune.getInfo().getSidChipBase(sidNum) : 0;
			break;
		case 2:
			forcedStereoTune = emulation.isForce3SIDTune();
			forcedSidBase = emulation.getThirdSIDBase();
			tuneChipBase = tune != null ? tune.getInfo().getSidChipBase(sidNum) : 0;
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		if (forcedStereoTune) {
			return forcedSidBase;
		} else {
			return tuneChipBase;
		}
	}

	/**
	 * Gets the audio framerate of this AudioConfig.
	 * 
	 * @return The audio framerate of this AudioConfig.
	 */
	public int getFrameRate() {
		return frameRate;
	}

	/**
	 * Return the desired size of buffer used at one time. This is often smaller
	 * than the whole buffer because doing this allows us to stay closer in sync
	 * with the audio production.
	 * 
	 * @return size of one chunk
	 */
	public int getChunkFrames() {
		return 1024 < bufferFrames ? 1024 : bufferFrames;
	}

	/**
	 * Gets the size of this AudioConfig's audio buffer in frames.
	 * 
	 * @return The size of this AudioConfig's audio buffer in frames.
	 */
	public int getBufferFrames() {
		return bufferFrames;
	}

	/**
	 * Gets the SID sampling method used by this AudioConfig.
	 * 
	 * @return The SID sampling method used by this AudioConfig.
	 */
	public SamplingMethod getSamplingMethod() {
		return samplingMethod;
	}

	public int getDevice() {
		return deviceIdx;
	}

}
