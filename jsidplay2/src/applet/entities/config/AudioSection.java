package applet.entities.config;

import java.io.File;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.swing.JFileChooser;

import resid_builder.resid.ISIDDefs.SamplingMethod;
import sidplay.ini.intf.IAudioSection;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigField;

@Embeddable
public class AudioSection implements IAudioSection {

	@ConfigDescription(descriptionKey = "AUDIO_FREQUENCY_DESC", toolTipKey = "AUDIO_FREQUENCY_TOOLTIP")
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
	@ConfigDescription(descriptionKey = "AUDIO_SAMPLING_DESC", toolTipKey = "AUDIO_SAMPLING_TOOLTIP")
	private SamplingMethod sampling = SamplingMethod.DECIMATE;

	@Override
	public SamplingMethod getSampling() {
		return this.sampling;
	}

	@Override
	public void setSampling(SamplingMethod method) {
		this.sampling = method;
	}

	@ConfigDescription(descriptionKey = "AUDIO_PLAY_ORIGINAL_DESC", toolTipKey = "AUDIO_PLAY_ORIGINAL_TOOLTIP")
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
	@ConfigDescription(descriptionKey = "AUDIO_MP3_FILE_DESC", toolTipKey = "AUDIO_MP3_FILE_TOOLTIP")
	private String mp3File;

	@Override
	public String getMp3File() {
		return this.mp3File;
	}

	@Override
	public void setMp3File(String recording) {
		this.mp3File = recording;
	}

	@ConfigDescription(descriptionKey = "AUDIO_LEFT_VOLUME_DESC", toolTipKey = "AUDIO_LEFT_VOLUME_TOOLTIP")
	private float leftVolume = 6.0f;

	@Override
	public float getLeftVolume() {
		return this.leftVolume;
	}

	@Override
	public void setLeftVolume(float volume) {
		this.leftVolume = volume;
	}

	@ConfigDescription(descriptionKey = "AUDIO_RIGHT_VOLUME_DESC", toolTipKey = "AUDIO_RIGHT_VOLUME_TOOLTIP")
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
