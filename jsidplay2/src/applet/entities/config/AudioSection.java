package applet.entities.config;

import java.io.File;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.swing.JFileChooser;

import resid_builder.resid.ISIDDefs.SamplingMethod;
import sidplay.ini.intf.IAudioSection;
import applet.config.annotations.ConfigField;

@Embeddable
public class AudioSection implements IAudioSection {

	private int frequency = 48000;

	@Override
	public int getFrequency() {
		return this.frequency;
	}

	@Override
	public void setFrequency(int freq) {
		this.frequency = freq;
	}

	@Enumerated(EnumType.STRING)
	private SamplingMethod sampling = SamplingMethod.DECIMATE;

	@Override
	public SamplingMethod getSampling() {
		return this.sampling;
	}

	@Override
	public void setSampling(SamplingMethod method) {
		this.sampling = method;
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

	@ConfigField(uiClass = File.class, filter = JFileChooser.FILES_ONLY)
	private String mp3File;

	@Override
	public String getMp3File() {
		return this.mp3File;
	}

	@Override
	public void setMp3File(String recording) {
		this.mp3File = recording;
	}

	private float leftVolume = 6.0f;

	@Override
	public float getLeftVolume() {
		return this.leftVolume;
	}

	@Override
	public void setLeftVolume(float volume) {
		this.leftVolume = volume;
	}

	private float rightVolume = 6.0f;

	@Override
	public float getRightVolume() {
		return this.rightVolume;
	}

	@Override
	public void setRightVolume(float volume) {
		this.rightVolume = volume;
	}

}
