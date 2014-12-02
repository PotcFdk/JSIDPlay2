package ui.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.common.SamplingMethod;
import sidplay.audio.Audio;
import sidplay.ini.intf.IAudioSection;

@Embeddable
public class AudioSection implements IAudioSection {

	private Audio audio = Audio.SOUNDCARD;

	@Enumerated(EnumType.STRING)
	@Override
	public Audio getAudio() {
		return audio;
	}

	@Override
	public void setAudio(Audio audio) {
		this.audio = audio;
	}

	private String sidDriver = "/libsidplay/sidtune/psiddriver.asm";
	
	@Override
	public String getSidDriver() {
		return sidDriver;
	}
	
	@Override
	public void setSidDriver(final String sidDriver) {
		this.sidDriver = sidDriver;
	}

	private int device = 0;

	@Override
	public int getDevice() {
		return device;
	}

	@Override
	public void setDevice(int device) {
		this.device = device;
	}

	private int frequency = 48000;

	@Override
	public int getFrequency() {
		return this.frequency;
	}

	@Override
	public void setFrequency(int freq) {
		this.frequency = freq;
	}

	private SamplingMethod sampling = SamplingMethod.DECIMATE;

	@Enumerated(EnumType.STRING)
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

	private float leftBalance = 0f;

	@Override
	public float getLeftBalance() {
		return this.leftBalance;
	}

	@Override
	public void setLeftBalance(float balance) {
		this.leftBalance = balance;
	}

	private float rightBalance = 1f;

	@Override
	public float getRightBalance() {
		return this.rightBalance;
	}

	@Override
	public void setRightBalance(float right) {
		this.rightBalance = right;
	}

}
