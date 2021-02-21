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
	private int audioBufferSize, bufferFrames;

	/**
	 * Return a detached AudioConfig instance corresponding to current
	 * parameters.<BR>
	 * <B>Note:</B> The number of audio channels is always two to support stereo
	 * tunes and to play mono tunes as stereo (fake stereo).
	 *
	 * @param audioSection audio configuration
	 *
	 */
	public AudioConfig(final IAudioSection audioSection) {
		this(audioSection.getSamplingRate().getFrequency(), 2, audioSection.getAudioBufferSize());
	}

	/**
	 * This instance represents the requested audio configuration.<BR>
	 *
	 * @param frameRate       The desired audio frame rate.
	 * @param channels        The number of audio channels to use.
	 * @param audioBufferSize The audio buffer size (null for reasonable default).
	 */
	public AudioConfig(final int frameRate, final int channels, Integer audioBufferSize) {
		this.frameRate = frameRate;
		this.channels = channels;
		this.bufferFrames = this.audioBufferSize = audioBufferSize;
	}

	/**
	 * Gets the audio frame rate of this AudioConfig.
	 *
	 * @return The audio frame rate of this AudioConfig.
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
	public final int getChunkFrames() {
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

	public final void setAudioBufferSize(int audioBufferSize) {
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
	 * <B>Note:</B> Java Linux ALSA Sound System is awful!<BR>
	 * Best results after numerous tests (Win/Linux, Java 8/11, 44.1K..96K, 1x..32x
	 * fast forward)<BR>
	 * 1024=responsiveness vs. 16384=stable audio since Java11 on Linux
	 *
	 * @return platform dependent default buffer size
	 */
	public static final int getDefaultBufferSize() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("nux") >= 0) {
			return 16384;
		} else {
			return 2048;
		}
	}

}
