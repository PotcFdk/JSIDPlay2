package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.sidtune.SidTune;

public class AudioNull extends AudioDriver {

	protected ByteBuffer sampleBuffer;

	@Override
	public void open(AudioConfig cfg, SidTune tune)
			throws LineUnavailableException, UnsupportedAudioFileException,
			IOException {
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * 2
				* cfg.channels);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
	}

	@Override
	public void pause() {
	}

	@Override
	public void close() {
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

}
