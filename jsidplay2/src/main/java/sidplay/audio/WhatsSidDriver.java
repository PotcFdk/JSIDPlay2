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
 * This is the analyzing part. Use WhatsSidMatcherDriver to match.
 * 
 * @author ken
 *
 */
public class WhatsSidDriver extends WhatsSidBaseDriver {

	private ByteBuffer sampleBuffer;

	private ByteArrayOutputStream out;

	private String recordingFilename;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		this.recordingFilename = recordingFilename;
		out = new ByteArrayOutputStream();

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
		System.out.println("Analyzing: " + recordingFilename);
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
		songNrToNameMap.put(nrSong, new File(recordingFilename).getName());
		makeSpectrum(out, nrSong, false);
		nrSong++;
		System.out.println("Analyzing End");
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
