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

/**
 * Audio configuration (frame rate, channels, etc.)
 * 
 * @author ken
 *
 */
public class AudioConfig {
	private final int frameRate;
	private final int channels;
	private int bufferFrames;
	private final int deviceIdx;
	private int audioBufferSize;

	/**
	 * This instance represents the requested audio configuration
	 * 
	 * @param frameRate       The desired audio frame rate.
	 * @param channels        The number of audio channels to use.
	 * @param deviceIdx       The sound device number.
	 * @param audioBufferSize The audio buffer size (null for reasonable default).
	 */
	public AudioConfig(final int frameRate, final int channels, final int deviceIdx, Integer audioBufferSize) {
		this.frameRate = frameRate;
		this.channels = channels;
		this.deviceIdx = deviceIdx;
		// Java Linux ALSA Sound System is awful!
		// Best results after numerous tests (Win/Linux, Java 8/11, 44.1K..96K, 1x..32x)
		if (audioBufferSize != null && audioBufferSize.intValue() >= 1024) {
			// JSIDPlay needs configuration (1024=responsiveness vs. 16384=stable audio)
			this.audioBufferSize = audioBufferSize;
			this.bufferFrames = this.audioBufferSize;
		} else {
			// JSIDDevice requires a small audio buffer
			this.audioBufferSize = 2048;
			this.bufferFrames = this.audioBufferSize;
		}
	}

	/**
	 * Return a detached AudioConfig instance corresponding to current
	 * parameters.<BR>
	 * <B>Note:</B> The number of audio channels is always two to support stereo
	 * tunes and to play mono tunes as stereo (fake stereo).
	 * 
	 * @param audio audio configuration
	 * 
	 * @return AudioConfig for current specification
	 */
	public static AudioConfig getInstance(final IAudioSection audio) {
		return new AudioConfig(audio.getSamplingRate().getFrequency(), 2, audio.getDevice(),
				audio.getAudioBufferSize());
	}

	/**
	 * Gets the audio framerate of this AudioConfig.
	 * 
	 * @return The audio framerate of this AudioConfig.
	 */
	public final int getFrameRate() {
		return frameRate;
	}

	/**
	 * Return the desired size of buffer used at one time. This is often smaller
	 * than the whole buffer because doing this allows us to stay closer in sync
	 * with the audio production.
	 * 
	 * <B>Note:</B>Do not choose too small values here: test with 96kHz and 32x fast
	 * forward!
	 * 
	 * <B>Note:</B> Current implementation uses exactly the same size as
	 * bufferFrames as a result after numerous tests!
	 * 
	 * @return size of one chunk
	 */
	public int getChunkFrames() {
		return Math.min(audioBufferSize, bufferFrames);
	}

	/**
	 * Gets the size of this AudioConfig's audio buffer in frames.
	 * 
	 * @return The size of this AudioConfig's audio buffer in frames.
	 */
	public final int getBufferFrames() {
		return bufferFrames;
	}

	/**
	 * The actual buffer size for the open audio line may differ from the requested
	 * buffer size, therefore the setter<BR>
	 * 
	 * <B>Note:</B> We make the sample buffer size divisible by 64 to ensure that
	 * all fast forward factors can be handled. (32x speed, 2 channels).<BR>
	 * <B>Note:</B> Must be greater or equal than the calculated chunk size!
	 * 
	 * @param bufferFrames available buffer frames
	 */
	public final void setBufferFrames(final int bufferFrames) {
		this.bufferFrames = bufferFrames;
	}

	public void setAudioBufferSize(int audioBufferSize) {
		this.audioBufferSize = audioBufferSize;
	}
	
	/**
	 * Get number of audio channels
	 * 
	 * @return audio channels
	 */
	public final int getChannels() {
		return channels;
	}

	/**
	 * Get currently used audio device
	 * 
	 * @return audio device
	 */
	public final int getDevice() {
		return deviceIdx;
	}

}
