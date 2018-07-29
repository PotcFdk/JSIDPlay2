package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_ORIGINAL;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_VOLUME;

import java.io.File;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;
import ui.common.FileToStringConverter;

@Embeddable
public class AudioSection implements IAudioSection {

	public AudioSection() {
		Bindings.bindBidirectional(this.mp3, mp3File, new FileToStringConverter());
	}

	private ObjectProperty<Audio> audio = new SimpleObjectProperty<Audio>(DEFAULT_AUDIO);

	@Enumerated(EnumType.STRING)
	@Override
	public Audio getAudio() {
		return audio.get();
	}

	@Override
	public void setAudio(Audio audio) {
		this.audio.set(audio);
	}

	public ObjectProperty<Audio> audioProperty() {
		return audio;
	}

	private IntegerProperty device = new SimpleIntegerProperty(DEFAULT_DEVICE);

	@Override
	public int getDevice() {
		return device.get();
	}

	@Override
	public void setDevice(int device) {
		this.device.set(device);
	}

	public IntegerProperty deviceProperty() {
		return device;
	}

	private ObjectProperty<SamplingRate> samplingRate = new SimpleObjectProperty<SamplingRate>(DEFAULT_SAMPLING_RATE);

	@Enumerated(EnumType.STRING)
	@Override
	public SamplingRate getSamplingRate() {
		return this.samplingRate.get();
	}

	@Override
	public void setSamplingRate(SamplingRate samplingRate) {
		this.samplingRate.set(samplingRate);
	}

	public ObjectProperty<SamplingRate> samplingRateProperty() {
		return samplingRate;
	}

	private ObjectProperty<SamplingMethod> sampling = new SimpleObjectProperty<SamplingMethod>(DEFAULT_SAMPLING);

	@Enumerated(EnumType.STRING)
	@Override
	public SamplingMethod getSampling() {
		return this.sampling.get();
	}

	@Override
	public void setSampling(SamplingMethod method) {
		this.sampling.set(method);
	}

	public ObjectProperty<SamplingMethod> samplingProperty() {
		return sampling;
	}

	private BooleanProperty playOriginal = new SimpleBooleanProperty(DEFAULT_PLAY_ORIGINAL);

	@Override
	public boolean isPlayOriginal() {
		return playOriginal.get();
	}

	@Override
	public void setPlayOriginal(boolean original) {
		this.playOriginal.set(original);
	}

	public BooleanProperty playOriginalProperty() {
		return playOriginal;
	}

	private ObjectProperty<File> mp3File = new SimpleObjectProperty<File>();
	private StringProperty mp3 = new SimpleStringProperty();

	public ObjectProperty<File> mp3FileProperty() {
		return mp3File;
	}

	@Override
	public String getMp3File() {
		return this.mp3.get();
	}

	@Override
	public void setMp3File(String recording) {
		this.mp3.set(recording);
	}

	private FloatProperty mainVolume = new SimpleFloatProperty(DEFAULT_MAIN_VOLUME);

	public FloatProperty mainVolumeProperty() {
		return mainVolume;
	}

	@Override
	public float getMainVolume() {
		return this.mainVolume.get();
	}

	@Override
	public void setMainVolume(float volume) {
		this.mainVolume.set(volume);
	}

	private FloatProperty secondVolume = new SimpleFloatProperty(DEFAULT_SECOND_VOLUME);

	public FloatProperty secondVolumeProperty() {
		return secondVolume;
	}

	@Override
	public float getSecondVolume() {
		return this.secondVolume.get();
	}

	@Override
	public void setSecondVolume(float volume) {
		this.secondVolume.set(volume);
	}

	private FloatProperty thirdVolume = new SimpleFloatProperty(DEFAULT_THIRD_VOLUME);

	public FloatProperty thirdVolumeProperty() {
		return thirdVolume;
	}

	@Override
	public float getThirdVolume() {
		return this.thirdVolume.get();
	}

	@Override
	public void setThirdVolume(float volume) {
		this.thirdVolume.set(volume);
	}

	private FloatProperty mainBalance = new SimpleFloatProperty(DEFAULT_MAIN_BALANCE);

	public FloatProperty mainBalanceProperty() {
		return mainBalance;
	}

	@Override
	public float getMainBalance() {
		return this.mainBalance.get();
	}

	@Override
	public void setMainBalance(float balance) {
		this.mainBalance.set(balance);
	}

	private FloatProperty secondBalance = new SimpleFloatProperty(DEFAULT_SECOND_BALANCE);

	public FloatProperty secondBalanceProperty() {
		return secondBalance;
	}

	@Override
	public float getSecondBalance() {
		return this.secondBalance.get();
	}

	@Override
	public void setSecondBalance(float right) {
		this.secondBalance.set(right);
	}

	private FloatProperty thirdBalance = new SimpleFloatProperty(DEFAULT_THIRD_BALANCE);

	public FloatProperty thirdBalanceProperty() {
		return thirdBalance;
	}

	@Override
	public float getThirdBalance() {
		return this.thirdBalance.get();
	}

	@Override
	public void setThirdBalance(float third) {
		this.thirdBalance.set(third);
	}

	private FloatProperty mainDelay = new SimpleFloatProperty(DEFAULT_MAIN_DELAY);

	public FloatProperty mainDelayProperty() {
		return mainDelay;
	}

	@Override
	public float getMainDelay() {
		return this.mainDelay.get();
	}

	@Override
	public void setMainDelay(float delay) {
		this.mainDelay.set(delay);
	}

	private FloatProperty secondDelay = new SimpleFloatProperty(DEFAULT_SECOND_DELAY);

	public FloatProperty secondDelayProperty() {
		return secondDelay;
	}

	@Override
	public float getSecondDelay() {
		return this.secondDelay.get();
	}

	@Override
	public void setSecondDelay(float delay) {
		this.secondDelay.set(delay);
	}

	private FloatProperty thirdDelay = new SimpleFloatProperty(DEFAULT_THIRD_DELAY);

	public FloatProperty thirdDelayProperty() {
		return this.thirdDelay;
	}

	@Override
	public float getThirdDelay() {
		return this.thirdDelay.get();
	}

	@Override
	public void setThirdDelay(float delay) {
		this.thirdDelay.set(delay);
	}

	private IntegerProperty bufferSize = new SimpleIntegerProperty(DEFAULT_BUFFER_SIZE);

	public IntegerProperty bufferSizeProperty() {
		return bufferSize;
	}

	@Override
	public int getBufferSize() {
		return bufferSize.get();
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize.set(bufferSize);
	}
}
