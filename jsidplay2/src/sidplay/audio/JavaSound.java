package sidplay.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class JavaSound extends AudioDriver {
	private AudioFormat audioFormat;
	private SourceDataLine dataLine;

	protected ByteBuffer sampleBuffer;

	@Override
	public synchronized void open(final AudioConfig cfg) throws LineUnavailableException {
		audioFormat = new AudioFormat(cfg.frameRate, 16, cfg.channels, true, false);
		dataLine = AudioSystem.getSourceDataLine(audioFormat);
		dataLine.open(dataLine.getFormat(), cfg.bufferFrames * 2 * cfg.channels);
		cfg.bufferFrames = dataLine.getBufferSize() / 2 / cfg.channels;
		
		/* Write to audio device often.
		 * We make the sample buffer size divisible by 64 to ensure that all
		 * fast forward factors can be handled. (32x speed, 2 channels)
		 */
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * 2 * cfg.channels);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public synchronized void write() throws InterruptedException {
		if (! dataLine.isActive()) {
			dataLine.start();
		}

		final int len;
		if (fastForward > 1) {
			sampleBuffer.rewind();
			int newLen = 0;
			int[] val = new int[audioFormat.getChannels()];
			int j = 0;
			/* for each short-formatted sample in the buffer... */
			while (sampleBuffer.position() < sampleBuffer.capacity()) {
				/* accumulate each interleaved channel into its own accumulator */
				for (int c = 0; c < audioFormat.getChannels(); c ++) {
					val[c] += sampleBuffer.getShort();
				}

				/* once enough samples have been accumulated, write one to output */
				j ++;
				if (j == fastForward) {
					j = 0;

					for (int c = 0; c < audioFormat.getChannels(); c ++) {
						sampleBuffer.putShort(newLen, (short) (val[c] / fastForward));
						newLen += 2;
					}

					/* zero accumulator */
					Arrays.fill(val, 0);
				}
			}
			len = newLen;
		} else {
			len = sampleBuffer.capacity();
		}

		int bytesWritten = dataLine.write(sampleBuffer.array(), 0, len);
		if (bytesWritten != len) {
			throw new InterruptedException();
		}
	}

	/**
	 * Estimate the length of audio data before we run out
	 * 
	 * @return playback time in ms
	 */
	public synchronized int getRemainingPlayTime() {
		int bytesPerFrame = dataLine.getFormat().getChannels() * 2;
		int framesPlayed = dataLine.available() / bytesPerFrame;
		int framesTotal = dataLine.getBufferSize() / bytesPerFrame;
		int framesNotYetPlayed = framesTotal - framesPlayed;
		return framesNotYetPlayed * 1000 / framesTotal;
	}
	
	@Override
	public synchronized void pause() {
		if (dataLine.isActive()) {
			dataLine.drain();
			dataLine.stop();
		}
	}

	@Override
	public synchronized void close() {
		if (dataLine == null) {
			return;
		}

		if (dataLine.isActive()) {
			dataLine.drain();
			dataLine.stop();
		}
		if (dataLine.isOpen()) {
			/* Fails with PulseAudio. Workaround, don't know why, might not matter. */
			try {
				dataLine.close();
			} catch (RuntimeException rte) {
			}
		}
	}

	public AudioFormat getAudioFormat() {
		return audioFormat;
	}
	
	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}
}
