package ui.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.common.PSIDDriver;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;

@Embeddable
public class AudioSection implements IAudioSection {

	public static final PSIDDriver DEFAULT_SID_DRIVER = PSIDDriver.JSIDPLAY2;

	private Audio audio = DEFAULT_AUDIO;

	@Enumerated(EnumType.STRING)
	@Override
	public Audio getAudio() {
		return audio;
	}

	@Override
	public void setAudio(Audio audio) {
		this.audio = audio;
	}

	private PSIDDriver sidDriver = DEFAULT_SID_DRIVER;

	@Enumerated(EnumType.STRING)
	public PSIDDriver getSidDriver() {
		return sidDriver;
	}

	public void setSidDriver(final PSIDDriver sidDriver) {
		this.sidDriver = sidDriver;
	}

	private int device = DEFAULT_DEVICE;

	@Override
	public int getDevice() {
		return device;
	}

	@Override
	public void setDevice(int device) {
		this.device = device;
	}

	private SamplingRate samplingRate = DEFAULT_SAMPLING_RATE;

	@Enumerated(EnumType.STRING)
	@Override
	public SamplingRate getSamplingRate() {
		return this.samplingRate;
	}

	@Override
	public void setSamplingRate(SamplingRate samplingRate) {
		this.samplingRate = samplingRate;
	}

	private SamplingMethod sampling = DEFAULT_SAMPLING;

	@Enumerated(EnumType.STRING)
	@Override
	public SamplingMethod getSampling() {
		return this.sampling;
	}

	@Override
	public void setSampling(SamplingMethod method) {
		this.sampling = method;
	}

	private boolean playOriginal = DEFAULT_PLAY_ORIGINAL;

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

	private float mainVolume = DEFAULT_MAIN_VOLUME;

	@Override
	public float getMainVolume() {
		return this.mainVolume;
	}

	@Override
	public void setMainVolume(float volume) {
		this.mainVolume = volume;
	}

	private float secondVolume = DEFAULT_SECOND_VOLUME;

	@Override
	public float getSecondVolume() {
		return this.secondVolume;
	}

	@Override
	public void setSecondVolume(float volume) {
		this.secondVolume = volume;
	}

	private float thirdVolume = DEFAULT_THIRD_VOLUME;

	@Override
	public float getThirdVolume() {
		return this.thirdVolume;
	}

	@Override
	public void setThirdVolume(float volume) {
		this.thirdVolume = volume;
	}

	private float mainBalance = DEFAULT_MAIN_BALANCE;

	@Override
	public float getMainBalance() {
		return this.mainBalance;
	}

	@Override
	public void setMainBalance(float balance) {
		this.mainBalance = balance;
	}

	private float secondBalance = DEFAULT_SECOND_BALANCE;

	@Override
	public float getSecondBalance() {
		return this.secondBalance;
	}

	@Override
	public void setSecondBalance(float right) {
		this.secondBalance = right;
	}

	private float thirdBalance = DEFAULT_THIRD_BALANCE;

	@Override
	public float getThirdBalance() {
		return this.thirdBalance;
	}

	@Override
	public void setThirdBalance(float third) {
		this.thirdBalance = third;
	}

	private int bufferSize = DEFAULT_BUFFER_SIZE;

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
}
