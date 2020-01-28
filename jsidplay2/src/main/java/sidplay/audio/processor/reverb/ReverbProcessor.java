package sidplay.audio.processor.reverb;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.AudioConfig;
import sidplay.audio.processor.AudioProcessor;

public class ReverbProcessor implements AudioProcessor {

	private IConfig config;
	private int sampleRate;
	private int numberOfChannels;
	private SchroederReverb reverb;

	public ReverbProcessor(IConfig config) {
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
		if (reverb == null) {
			reverb = new SchroederReverb(sampleRate, numberOfChannels, sampleBuffer.capacity());
		}
		if (reverb.getComb1Delay() != audioSection.getReverbComb1Delay()) {
			reverb.setComb1Delay(audioSection.getReverbComb1Delay());
		}
		if (reverb.getComb2Delay() != audioSection.getReverbComb2Delay()) {
			reverb.setComb2Delay(audioSection.getReverbComb2Delay());
		}
		if (reverb.getComb3Delay() != audioSection.getReverbComb3Delay()) {
			reverb.setComb3Delay(audioSection.getReverbComb3Delay());
		}
		if (reverb.getComb4Delay() != audioSection.getReverbComb4Delay()) {
			reverb.setComb4Delay(audioSection.getReverbComb4Delay());
		}
		if (reverb.getAllpass1Delay() != audioSection.getReverbAllPass1Delay()) {
			reverb.setAllpass1Delay(audioSection.getReverbAllPass1Delay());
		}
		if (reverb.getAllpass2Delay() != audioSection.getReverbAllPass2Delay()) {
			reverb.setAllpass2Delay(audioSection.getReverbAllPass2Delay());
		}
		if (reverb.getSustainInMs() != audioSection.getReverbSustainDelay()) {
			reverb.setSustainInMs(audioSection.getReverbSustainDelay());
		}
		if (reverb.getMix() != audioSection.getReverbDryWetMix()) {
			reverb.setDryWetMix(audioSection.getReverbDryWetMix());
		}
		if (!audioSection.getReverbBypass()) {
			short[] dest = new short[sampleBuffer.position() >> 1];
			((Buffer) sampleBuffer).flip();
			sampleBuffer.asShortBuffer().get(dest);
			int newLen = reverb.doReverb(dest, dest.length);
			sampleBuffer.asShortBuffer().put(dest, 0, newLen);
			((Buffer) sampleBuffer).position(newLen << 1);
		}
	}

}
