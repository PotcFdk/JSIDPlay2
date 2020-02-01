package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_AVI_COMPRESSION_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_CBR;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_DRY_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_FEEDBACK_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_WET_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_ORIGINAL;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_ALL_PASS1_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_ALL_PASS2_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB1_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB2_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB3_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB4_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_DRY_WET_MIX;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_SUSTAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_VBR;
import static sidplay.ini.IniDefaults.DEFAULT_VBR_QUALITY;

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

	private IntegerProperty mainDelay = new SimpleIntegerProperty(DEFAULT_MAIN_DELAY);

	public IntegerProperty mainDelayProperty() {
		return mainDelay;
	}

	@Override
	public int getMainDelay() {
		return this.mainDelay.get();
	}

	@Override
	public void setMainDelay(int delay) {
		this.mainDelay.set(delay);
	}

	private IntegerProperty secondDelay = new SimpleIntegerProperty(DEFAULT_SECOND_DELAY);

	public IntegerProperty secondDelayProperty() {
		return secondDelay;
	}

	@Override
	public int getSecondDelay() {
		return this.secondDelay.get();
	}

	@Override
	public void setSecondDelay(int delay) {
		this.secondDelay.set(delay);
	}

	private IntegerProperty thirdDelay = new SimpleIntegerProperty(DEFAULT_THIRD_DELAY);

	public IntegerProperty thirdDelayProperty() {
		return this.thirdDelay;
	}

	@Override
	public int getThirdDelay() {
		return this.thirdDelay.get();
	}

	@Override
	public void setThirdDelay(int delay) {
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

	private ObjectProperty<Integer> audioBufferSize = new SimpleObjectProperty<>(DEFAULT_AUDIO_BUFFER_SIZE);

	public ObjectProperty<Integer> audioBufferSizeProperty() {
		return audioBufferSize;
	}

	@Override
	public int getAudioBufferSize() {
		return audioBufferSize.get();
	}

	@Override
	public void setAudioBufferSize(int audioBufferSize) {
		this.audioBufferSize.set(audioBufferSize);
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

	private ObjectProperty<Integer> cbrProperty = new SimpleObjectProperty<>(DEFAULT_CBR);

	@Override
	public int getCbr() {
		return cbrProperty.get();
	}

	@Override
	public void setCbr(int cbr) {
		this.cbrProperty.set(cbr);
	}

	public ObjectProperty<Integer> cbrProperty() {
		return cbrProperty;
	}

	private BooleanProperty vbrProperty = new SimpleBooleanProperty(DEFAULT_VBR);

	@Override
	public boolean isVbr() {
		return vbrProperty.get();
	}

	@Override
	public void setVbr(boolean vbr) {
		vbrProperty.set(vbr);
	}

	public BooleanProperty vbrProperty() {
		return vbrProperty;
	}

	private ObjectProperty<Integer> vbrQualityProperty = new SimpleObjectProperty<>(DEFAULT_VBR_QUALITY);

	@Override
	public int getVbrQuality() {
		return vbrQualityProperty.get();
	}

	@Override
	public void setVbrQuality(int vbrQuality) {
		this.vbrQualityProperty.set(vbrQuality);
	}

	public ObjectProperty<Integer> vbrQualityProperty() {
		return vbrQualityProperty;
	}

	private FloatProperty aviCompressionQualityProperty = new SimpleFloatProperty(DEFAULT_AVI_COMPRESSION_QUALITY);

	@Override
	public float getAviCompressionQuality() {
		return aviCompressionQualityProperty.get();
	}

	@Override
	public void setAviCompressionQuality(float aviCompressionQuality) {
		aviCompressionQualityProperty.set(aviCompressionQuality);
	}

	public FloatProperty aviCompressionQualityProperty() {
		return aviCompressionQualityProperty;
	}

	private BooleanProperty delayBypassProperty = new SimpleBooleanProperty(DEFAULT_DELAY_BYPASS);

	@Override
	public boolean getDelayBypass() {
		return delayBypassProperty.get();
	}

	@Override
	public void setDelayBypass(boolean delayBypass) {
		delayBypassProperty.set(delayBypass);
	}

	public BooleanProperty delayBypassProperty() {
		return delayBypassProperty;
	}

	private IntegerProperty delayProperty = new SimpleIntegerProperty(DEFAULT_DELAY);

	@Override
	public int getDelay() {
		return delayProperty.get();
	}

	@Override
	public void setDelay(int delay) {
		delayProperty.set(delay);
	}

	public IntegerProperty delayProperty() {
		return delayProperty;
	}

	private IntegerProperty delayDryLevelProperty = new SimpleIntegerProperty(DEFAULT_DELAY_DRY_LEVEL);

	@Override
	public int getDelayDryLevel() {
		return delayDryLevelProperty.get();
	}

	@Override
	public void setDelayDryLevel(int delayDryLevel) {
		delayDryLevelProperty.set(delayDryLevel);
	}

	public IntegerProperty delayDryLevelProperty() {
		return delayDryLevelProperty;
	}

	private IntegerProperty delayWetLevelProperty = new SimpleIntegerProperty(DEFAULT_DELAY_WET_LEVEL);

	@Override
	public int getDelayWetLevel() {
		return delayWetLevelProperty.get();
	}

	@Override
	public void setDelayWetLevel(int delayWetLevel) {
		delayWetLevelProperty.set(delayWetLevel);
	}

	public IntegerProperty delayWetLevelProperty() {
		return delayWetLevelProperty;
	}

	private IntegerProperty delayFeedbackLevelProperty = new SimpleIntegerProperty(DEFAULT_DELAY_FEEDBACK_LEVEL);

	@Override
	public int getDelayFeedbackLevel() {
		return delayFeedbackLevelProperty.get();
	}

	@Override
	public void setDelayFeedbackLevel(int delayFeedbackLevel) {
		delayFeedbackLevelProperty.set(delayFeedbackLevel);
	}

	public IntegerProperty delayFeedbackLevelProperty() {
		return delayFeedbackLevelProperty;
	}

	private BooleanProperty reverbBypassProperty = new SimpleBooleanProperty(DEFAULT_REVERB_BYPASS);

	@Override
	public boolean getReverbBypass() {
		return reverbBypassProperty.get();
	}

	@Override
	public void setReverbBypass(boolean reverbBypass) {
		reverbBypassProperty.set(reverbBypass);
	}

	public BooleanProperty reverbBypassProperty() {
		return reverbBypassProperty;
	}

	private FloatProperty reverbComb1DelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_COMB1_DELAY);

	@Override
	public float getReverbComb1Delay() {
		return reverbComb1DelayProperty.get();
	}

	@Override
	public void setReverbComb1Delay(float reverbComb1Delay) {
		reverbComb1DelayProperty.set(reverbComb1Delay);
	}

	public FloatProperty reverbComb1DelayProperty() {
		return reverbComb1DelayProperty;
	}

	private FloatProperty reverbComb2DelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_COMB2_DELAY);

	@Override
	public float getReverbComb2Delay() {
		return reverbComb2DelayProperty.get();
	}

	@Override
	public void setReverbComb2Delay(float reverbComb2Delay) {
		reverbComb2DelayProperty.set(reverbComb2Delay);
	}

	public FloatProperty reverbComb2DelayProperty() {
		return reverbComb2DelayProperty;
	}

	private FloatProperty reverbComb3DelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_COMB3_DELAY);

	@Override
	public float getReverbComb3Delay() {
		return reverbComb3DelayProperty.get();
	}

	@Override
	public void setReverbComb3Delay(float reverbComb3Delay) {
		reverbComb3DelayProperty.set(reverbComb3Delay);
	}

	public FloatProperty reverbComb3DelayProperty() {
		return reverbComb3DelayProperty;
	}

	private FloatProperty reverbComb4DelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_COMB4_DELAY);

	@Override
	public float getReverbComb4Delay() {
		return reverbComb4DelayProperty.get();
	}

	@Override
	public void setReverbComb4Delay(float reverbComb4Delay) {
		reverbComb4DelayProperty.set(reverbComb4Delay);
	}

	public FloatProperty reverbComb4DelayProperty() {
		return reverbComb4DelayProperty;
	}

	private FloatProperty reverbAllPass1DelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_ALL_PASS1_DELAY);

	@Override
	public float getReverbAllPass1Delay() {
		return reverbAllPass1DelayProperty.get();
	}

	@Override
	public void setReverbAllPass1Delay(float reverbAllPass1Delay) {
		reverbAllPass1DelayProperty.set(reverbAllPass1Delay);
	}

	public FloatProperty reverbAllPass1DelayProperty() {
		return reverbAllPass1DelayProperty;
	}

	private FloatProperty reverbAllPass2DelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_ALL_PASS2_DELAY);

	@Override
	public float getReverbAllPass2Delay() {
		return reverbAllPass2DelayProperty.get();
	}

	@Override
	public void setReverbAllPass2Delay(float reverbAllPass2Delay) {
		reverbAllPass2DelayProperty.set(reverbAllPass2Delay);
	}

	public FloatProperty reverbAllPass2DelayProperty() {
		return reverbAllPass2DelayProperty;
	}

	private FloatProperty reverbSustainDelayProperty = new SimpleFloatProperty(DEFAULT_REVERB_SUSTAIN_DELAY);

	@Override
	public float getReverbSustainDelay() {
		return reverbSustainDelayProperty.get();
	}

	@Override
	public void setReverbSustainDelay(float reverbSustainDelay) {
		reverbSustainDelayProperty.set(reverbSustainDelay);
	}

	public FloatProperty reverbSustainDelayProperty() {
		return reverbSustainDelayProperty;
	}

	private FloatProperty reverbDryWetMixProperty = new SimpleFloatProperty(DEFAULT_REVERB_DRY_WET_MIX);

	@Override
	public float getReverbDryWetMix() {
		return reverbDryWetMixProperty.get();
	}

	@Override
	public void setReverbDryWetMix(float reverbDryWetMix) {
		reverbDryWetMixProperty.set(reverbDryWetMix);
	}

	public FloatProperty reverbDryWetMixProperty() {
		return reverbDryWetMixProperty;
	}

}
