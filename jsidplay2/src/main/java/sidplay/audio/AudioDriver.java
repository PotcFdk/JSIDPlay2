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

public interface AudioDriver {

	/**
	 * Open audio interface.
	 * 
	 * The audio parameters may be manipulated by open().
	 * 
	 * @param cfg
	 *            Configuration requested.
	 * @param recordingFilename
	 *            name for a recording
	 * @throws IOException
	 */
	void open(AudioConfig cfg, String recordingFilename) throws IOException;

	/**
	 * Write the complete contents of ByteBuffer to audio device.
	 * 
	 * @throws InterruptedException
	 */
	void write() throws InterruptedException;

	/**
	 * Temporarily cease audio production, for instance if user paused the
	 * application. Some backends such as DirectSound end up looping the audio
	 * unless explicitly told to pause.
	 * 
	 * Audio will be resumed automatically on next write().
	 */
	void pause();

	/**
	 * Free the audio device. (Counterpart of open().)
	 */
	void close();

	/**
	 * Return the bytebuffer intended to hold the audio data.
	 * 
	 * The audio data is in interleaved format and has as many channels as given
	 * by the result of open(). Use putShort() to write 16-bit values. Don't
	 * call write() until you have filled the entire buffer with audio.
	 * 
	 * @return The buffer to write audio to.
	 */
	ByteBuffer buffer();

}
