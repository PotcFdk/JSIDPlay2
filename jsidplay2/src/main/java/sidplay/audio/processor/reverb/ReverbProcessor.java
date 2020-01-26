package sidplay.audio.processor.reverb;

import java.nio.ShortBuffer;

import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.AudioConfig;
import sidplay.audio.processor.AudioProcessor;

public class ReverbProcessor implements AudioProcessor {

	private IConfig config;
	private int sampleRate;
	private int numberOfChannels;
	private int SAMPLEBUFFERSIZE;
	private SchroederReverb reverb;

	public ReverbProcessor(IConfig config) {
		this.config = config;
	}

	@Override
	public void prepare(ShortBuffer sampleBuffer, AudioConfig cfg) {
		SAMPLEBUFFERSIZE = sampleBuffer.capacity();
		sampleRate = cfg.getFrameRate();
		numberOfChannels = cfg.getChannels();

		reverb = new SchroederReverb(sampleRate, numberOfChannels, SAMPLEBUFFERSIZE);
		reverb.setComb1Delay(20);
		reverb.setComb2Delay(30);
		reverb.setComb3Delay(40);
		reverb.setComb4Delay(50);
		reverb.setAllpass1Delay(2);
		reverb.setAllpass2Delay(4);

		reverb.setSustainInMs(800);
		reverb.setDryWetMix(2);
	}

	@Override
	public void process(ShortBuffer sampleBuffer) {
		IAudioSection audioSection = config.getAudioSection();
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
			int len = sampleBuffer.position();
			sampleBuffer.flip();
			short[] dest = new short[len];
			ShortBuffer.wrap(dest).put(sampleBuffer);
			int newLen = reverb.doReverb(dest, len);
			sampleBuffer.clear();
			sampleBuffer.put(dest, 0, newLen);
		}
	}

}
