package sidplay.audio.processor.delay;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.AudioConfig;
import sidplay.audio.processor.AudioProcessor;

public class DelayProcessor implements AudioProcessor {

	private IConfig config;

	private int sampleRate, numberOfChannels;

	private short[] delayBuffer;
	private int readIndex, writeIndex, delayBufferSize, delayOffset;

	private Integer delayInMs;

	public DelayProcessor(IConfig config) {
		this.config = config;
	}

	@Override
	public void prepare(AudioConfig cfg) {
		sampleRate = cfg.getFrameRate();
		numberOfChannels = cfg.getChannels();
	}

	@Override
	public void process(ByteBuffer sampleBuffer) {
		IAudioSection audioSection = config.getAudioSection();
		if (delayInMs == null || delayInMs != config.getAudioSection().getDelay()) {
			delayInMs = config.getAudioSection().getDelay();

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
				int inputSample = (int) sampleBuffer.getShort();
				int delaySample = (int) delayBuffer[readIndex++];
				int outputSample = ((inputSample * audioSection.getDelayDryLevel()) / 100)
						+ ((delaySample * audioSection.getDelayWetLevel()) / 100);

				if (outputSample > 32767)
					outputSample = 32767;
				else if (outputSample < -32768)
					outputSample = -32768;

				buffer.putShort((short) outputSample);

				inputSample += (delaySample * audioSection.getDelayFeedbackLevel()) / 100;

				if (inputSample > 32767)
					inputSample = 32767;
				else if (inputSample < -32768)
					inputSample = -32768;

				delayBuffer[writeIndex++] = (short) inputSample;

				readIndex %= delayBufferSize;
				writeIndex %= delayBufferSize;

			}
			((Buffer) sampleBuffer).flip();
			((Buffer) buffer).flip();
			sampleBuffer.put(buffer);
		}
	}

}
