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

import libsidplay.config.IAudioSection;
import resid_builder.SIDMixer;

public class AudioConfig {
	protected final int frameRate;
	protected final int channels;
	protected int bufferFrames;
	protected final int deviceIdx;

	/**
	 * This instance represents the requested audio configuration
	 * 
	 * @param frameRate
	 *            The desired audio framerate.
	 * @param channels
	 *            The number of audio channels to use.
	 * @param deviceIdx
	 *            The sound device number.
	 */
	protected AudioConfig(int frameRate, int channels, int deviceIdx) {
		this.frameRate = frameRate;
		this.channels = channels;
		this.deviceIdx = deviceIdx;
		/*
		 * We make the sample buffer size divisible by 64 to ensure that all
		 * fast forward factors can be handled. (32x speed, 2 channels)
		 */
		this.bufferFrames = (1 << SIDMixer.MAX_FAST_FORWARD) * channels * 64;
	}

	/**
	 * Return a detached AudioConfig instance corresponding to current
	 * parameters.<BR>
	 * <B>Note:</B> The number of audio channels is always two to support stereo
	 * tunes and to play mono tunes as stereo (fake stereo).
	 * 
	 * @param audio
	 *            audio configuration
	 * 
	 * @return AudioConfig for current specification
	 */
	public static AudioConfig getInstance(IAudioSection audio) {
		return new AudioConfig(audio.getSamplingRate().getFrequency(), 2,
				audio.getDevice());
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
		return Math.min(1024, bufferFrames);
	}

	/**
	 * Gets the size of this AudioConfig's audio buffer in frames.
	 * 
	 * @return The size of this AudioConfig's audio buffer in frames.
	 */
	public int getBufferFrames() {
		return bufferFrames;
	}

	public int getChannels() {
		return channels;
	}

	public int getDevice() {
		return deviceIdx;
	}

}
