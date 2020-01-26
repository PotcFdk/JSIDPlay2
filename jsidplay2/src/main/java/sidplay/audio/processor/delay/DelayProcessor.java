package sidplay.audio.processor.delay;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.AudioConfig;
import sidplay.audio.processor.AudioProcessor;

public class DelayProcessor implements AudioProcessor {

	private IConfig config;

	private int sampleRate, numberOfChannels;

	private short[] delayBuffer;
	private int readIndex, writeIndex, delayBufferSize, delayOffset;

	private int SAMPLEBUFFERSIZE;

	private int delayInMs;

	public DelayProcessor(IConfig config) {
		this.config = config;
	}

	@Override
	public void prepare(ShortBuffer sampleBuffer, AudioConfig cfg) {
		SAMPLEBUFFERSIZE = sampleBuffer.capacity();
		sampleRate = cfg.getFrameRate();
		numberOfChannels = cfg.getChannels();

		prepareDelayBuffer();
	}

	private void prepareDelayBuffer() {
		delayInMs = config.getAudioSection().getDelay();

		delayOffset = (delayInMs * sampleRate * numberOfChannels) / 1000;
		delayBufferSize = SAMPLEBUFFERSIZE + delayOffset;
		delayBuffer = new short[delayBufferSize];
		writeIndex = 0;
		readIndex = SAMPLEBUFFERSIZE;
	}

	@Override
	public void process(ShortBuffer sampleBuffer) {
		IAudioSection audioSection = config.getAudioSection();
		if (delayInMs != config.getAudioSection().getDelay()) {
			prepareDelayBuffer();
		}

		if (!audioSection.getDelayBypass() && delayOffset > 0) {
			int len = sampleBuffer.position();
			((Buffer) sampleBuffer).flip();
			ShortBuffer buffer = ShortBuffer.wrap(new short[len]);

			for (int i = 0; i < len; i++) {
				int inputSample = (int) sampleBuffer.get();
				int delaySample = (int) delayBuffer[readIndex++];
				int outputSample = ((inputSample * audioSection.getDelayDryLevel()) / 100)
						+ ((delaySample * audioSection.getDelayWetLevel()) / 100);

				if (outputSample > 32767)
					outputSample = 32767;
				else if (outputSample < -32768)
					outputSample = -32768;

				buffer.put((short) outputSample);

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
