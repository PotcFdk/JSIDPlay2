package sidplay.audio.processor.delay;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.processor.AudioProcessor;

public class DelayProcessor implements AudioProcessor {

	private int sampleRate, numberOfChannels;

	private short[] delayBuffer;
	private int readIndex, writeIndex, delayBufferSize, delayOffset;

	private Integer delayInMs;

	private IAudioSection audioSection;
	
	@Override
	public void configure(IAudioSection audioSection) {
		this.audioSection = audioSection;
	}

	@Override
	public void prepare(AudioConfig cfg) {
		sampleRate = cfg.getFrameRate();
		numberOfChannels = cfg.getChannels();
	}

	@Override
	public void process(ByteBuffer sampleBuffer) {
		if (delayInMs == null || delayInMs != audioSection.getDelay()) {
			delayInMs = audioSection.getDelay();

			delayOffset = (delayInMs * sampleRate * numberOfChannels) / 1000;
			delayBufferSize = (sampleBuffer.capacity() >> 1) + delayOffset;
			delayBuffer = new short[delayBufferSize];
			writeIndex = 0;
			readIndex = sampleBuffer.capacity() >> 1;
		}

		if (!audioSection.getDelayBypass() && delayOffset > 0) {
			int len = sampleBuffer.position();
			((Buffer) sampleBuffer).flip();
			ByteBuffer buffer = ByteBuffer.wrap(new byte[len]).order(sampleBuffer.order());

			for (int i = 0; i < len >> 1; i++) {
				int inputSample = sampleBuffer.getShort();
				int delaySample = delayBuffer[readIndex++];
				int outputSample = ((inputSample * audioSection.getDelayDryLevel()) / 100)
						+ ((delaySample * audioSection.getDelayWetLevel()) / 100);

				outputSample = Math.max(Math.min(outputSample, Short.MAX_VALUE), Short.MIN_VALUE);

				buffer.putShort((short) outputSample);

				inputSample += (delaySample * audioSection.getDelayFeedbackLevel()) / 100;

				inputSample = Math.max(Math.min(inputSample, Short.MAX_VALUE), Short.MIN_VALUE);

				delayBuffer[writeIndex++] = (short) inputSample;

				if (readIndex == delayBufferSize) {
					readIndex = 0;
				}
				if (writeIndex == delayBufferSize) {
					writeIndex = 0;
				}
			}
			((Buffer) sampleBuffer).flip();
			((Buffer) buffer).flip();
			sampleBuffer.put(buffer);
		}
	}

}
