package sidplay.audio.processor.distortion;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import builder.resid.SIDMixer;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.AudioConfig;
import sidplay.audio.processor.AudioProcessor;

public class DistortionProcessor implements AudioProcessor {

	private IConfig config;

	public DistortionProcessor(IConfig config) {
		this.config = config;
	}

	@Override
	public void prepare(ShortBuffer sampleBuffer, AudioConfig cfg) {
	}

	@Override
	public void process(ShortBuffer sampleBuffer) {
		IAudioSection audioSection = config.getAudioSection();
		if (!audioSection.getDistortionBypass() && audioSection.getDistortionThreshold() < 32768) {
			int len = sampleBuffer.position();
			((Buffer) sampleBuffer).flip();
			ShortBuffer buffer = ShortBuffer.wrap(new short[len]);

			for (int i = 0; i < len; i++) {
				int inputSample = (int) sampleBuffer.get();
				int outputSample = inputSample;

				if (outputSample > audioSection.getDistortionThreshold())
					outputSample = 32767;
				else if (outputSample < -audioSection.getDistortionThreshold())
					outputSample = -32768;

				float db = audioSection.getDistortionGain();

				double gain = SIDMixer.DECIBEL_TO_LINEAR(db);

				if (gain < 1)
					gain = -gain;

				buffer.put((short) (outputSample * gain));
			}
			((Buffer) sampleBuffer).flip();
			((Buffer) buffer).flip();
			sampleBuffer.put(buffer);
		}
	}

}
