package sidplay.audio;

import java.nio.ByteBuffer;

public class AudioNull extends AudioDriver {
	private ByteBuffer sampleBuffer;
	
	public AudioNull() {
	}

	@Override
	public void open(final AudioConfig cfg) {
		sampleBuffer = ByteBuffer.allocateDirect(cfg.getChunkFrames());
	}

	@Override
	public void write() {
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
