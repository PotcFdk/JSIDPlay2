package applet.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import resid_builder.resid.ISIDDefs.SamplingMethod;
import sidplay.ini.intf.IAudioSection;

@Embeddable
public class DbAudioSection implements IAudioSection {

	private int frequency;

	@Override
	public int getFrequency() {
		return this.frequency;
	}

	@Override
	public void setFrequency(int freq) {
		this.frequency = freq;
	}

	@Enumerated(EnumType.STRING)
	private SamplingMethod method;

	@Override
	public SamplingMethod getSampling() {
		return this.method;
	}

	@Override
	public void setSampling(SamplingMethod method) {
		this.method = method;
	}

	private boolean playOriginal;

	@Override
	public boolean isPlayOriginal() {
		return playOriginal;
	}

	@Override
	public void setPlayOriginal(boolean original) {
		this.playOriginal = original;
	}

	private String mp3File;

	@Override
	public String getMp3File() {
		return this.mp3File;
	}

	@Override
	public void setMp3File(String recording) {
		this.mp3File = recording;
	}

	private float leftVolume;

	@Override
	public float getLeftVolume() {
		return this.leftVolume;
	}

	@Override
	public void setLeftVolume(float volume) {
		this.leftVolume = volume;
	}

	private float rightVolume;

	@Override
	public float getRightVolume() {
		return this.rightVolume;
	}

	@Override
	public void setRightVolume(float volume) {
		this.rightVolume = volume;
	}

}
