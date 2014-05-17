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

import java.io.File;

import libsidplay.sidtune.SidTune;
import resid_builder.resid.SamplingMethod;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;

public class AudioConfig {
	protected int frameRate = 48000;
	protected int channels = 1;
	protected int bufferFrames = 4096;
	private SamplingMethod samplingMethod;
	private File tuneFile;
	private int songCount;
	private int currentSong;
	private String outputFilename;

	protected AudioConfig() {
	}

	/**
	 * This instance represents the requested audio configuration
	 * 
	 * @param frameRate
	 *            The desired audio framerate.
	 * @param channels
	 *            The number of audio channels to use.
	 */
	public AudioConfig(int frameRate, int channels,
			SamplingMethod samplingMethod) {
		this.frameRate = frameRate;
		this.channels = channels;
		this.samplingMethod = samplingMethod;
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
				audio.getSampling());
	}

	public static AudioConfig create(IConfig config, SidTune tune) {
		AudioConfig audioConfig = getInstance(config.getAudio(),
				isStereo(config, tune) ? 2 : 1);
		audioConfig.tuneFile = tune != null ? tune.getInfo().getFile() : null;
		audioConfig.songCount = tune != null ? tune.getInfo().getSongs() : 1;
		audioConfig.currentSong = tune != null ? tune.getInfo().getCurrentSong() : 1;
		audioConfig.outputFilename = tune != null ? tune.getOutputFilename()
				: null;
		return audioConfig;
	}

	public static boolean isStereo(IConfig config, SidTune tune) {
		return config.getEmulation().isForceStereoTune() || tune != null
				&& tune.getInfo().getSidChipBase2() != 0;
	}

	public static Integer getStereoAddress(IConfig config, SidTune tune) {
		if (config.getEmulation().isForceStereoTune()) {
			return config.getEmulation().getDualSidBase();
		} else if (tune != null && tune.getInfo().getSidChipBase2() != 0) {
			return tune.getInfo().getSidChipBase2();
		} else {
			return null;
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

	/**
	 * Get filename of the tune.
	 * 
	 * @return filename of the tune
	 */
	public final File getTuneFile() {
		return tuneFile;
	}

	/**
	 * Get song count of the tune.
	 * 
	 * @return song count
	 */
	public final int getSongCount() {
		return songCount;
	}

	/**
	 * Get the current song number.
	 * 
	 * @return current song
	 */
	public final int getCurrentSong() {
		return currentSong;
	}

	/**
	 * Get an output filename proposal.
	 * 
	 * @return output filename
	 */
	public final String getOutputFilename() {
		return outputFilename;
	}

}
