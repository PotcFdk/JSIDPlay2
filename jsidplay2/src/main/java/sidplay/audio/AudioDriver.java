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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.sidtune.SidTune;

public abstract class AudioDriver {
	protected Function<SidTune, String> recordingFilenameProvider;

	/**
	 * Open audio interface.
	 * 
	 * The audio parameters may be manipulated by open().
	 * 
	 * @param cfg
	 *            Configuration requested.
	 * @param tune 
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public abstract void open(AudioConfig cfg, SidTune tune) throws LineUnavailableException,
			UnsupportedAudioFileException, IOException;

	/**
	 * Write the complete contents of ByteBuffer to audio device.
	 * 
	 * @throws InterruptedException
	 */
	public abstract void write() throws InterruptedException;

	/**
	 * Temporarily cease audio production, for instance if user paused the
	 * application. Some backends such as DirectSound end up looping the audio
	 * unless explicitly told to pause.
	 * 
	 * Audio will be resumed automatically on next write().
	 */
	public abstract void pause();

	/**
	 * Free the audio device. (Counterpart of open().)
	 */
	public abstract void close();

	/**
	 * Return the bytebuffer intended to hold the audio data.
	 * 
	 * The audio data is in interleaved format and has as many channels as given
	 * by the result of open(). Use putShort() to write 16-bit values. Don't
	 * call write() until you have filled the entire buffer with audio.
	 * 
	 * @return The buffer to write audio to.
	 */
	public abstract ByteBuffer buffer();

	public void setRecordingFilenameProvider(
			Function<SidTune, String> recordingFilenameProvider) {
		this.recordingFilenameProvider = recordingFilenameProvider;
	}
}
