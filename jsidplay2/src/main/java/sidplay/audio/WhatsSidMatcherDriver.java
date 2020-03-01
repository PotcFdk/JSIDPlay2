package sidplay.audio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import sidplay.audio.whatssid.WhatsSidBaseDriver;

/**
 * Beta: Shazam like feature: Analyze tunes to recognize a currently played tune
 * 
 * This is the matching part. Use WhatsSidDriver to analyze beforehand.
 * 
 * @author ken
 *
 */
public class WhatsSidMatcherDriver extends WhatsSidBaseDriver {

	private ByteBuffer sampleBuffer;

	private ByteArrayOutputStream out;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		out = new ByteArrayOutputStream();

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
		System.out.println("Try to match=" + new File(recordingFilename).getName() + ", number of songs=" + nrSong);
	}

	@Override
	public void write() throws InterruptedException {
		int length = sampleBuffer.position();
		byte[] buffer = new byte[length];
		sampleBuffer.flip();
		sampleBuffer.get(buffer, 0, length);
		out.write(buffer, 0, length);
	}

	@Override
	public void close() {
		byte b[] = out.toByteArray();
		System.out.println("close: bytes.length=" + b.length);
		makeSpectrum(out, nrSong, true);
		match();
		System.out.println("Match End");
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return true;
	}

}
