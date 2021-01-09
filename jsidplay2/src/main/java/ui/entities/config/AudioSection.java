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
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;
import ui.common.FileToStringConverter;
import ui.common.ShadowField;

@Embeddable
public class AudioSection implements IAudioSection {

	public AudioSection() {
		Bindings.bindBidirectional(this.mp3.property(), mp3File.property(), new FileToStringConverter());
	}

	private ShadowField<ObjectProperty<Audio>, Audio> audio = new ShadowField<>(DEFAULT_AUDIO,
			SimpleObjectProperty::new);

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
		return audio.property();
	}

	private ShadowField<IntegerProperty, Number> device = new ShadowField<>(DEFAULT_DEVICE,
			number -> new SimpleIntegerProperty(number.intValue()));

	@Override
	public int getDevice() {
		return device.get().intValue();
	}

	@Override
	public void setDevice(int device) {
		this.device.set(device);
	}

	public IntegerProperty deviceProperty() {
		return device.property();
	}

	private ShadowField<ObjectProperty<SamplingRate>, SamplingRate> samplingRate = new ShadowField<>(
			DEFAULT_SAMPLING_RATE, SimpleObjectProperty::new);

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
		return samplingRate.property();
	}

	private ShadowField<ObjectProperty<SamplingMethod>, SamplingMethod> sampling = new ShadowField<>(DEFAULT_SAMPLING,
			SimpleObjectProperty::new);

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
		return sampling.property();
	}

	private ShadowField<FloatProperty, Number> mainVolume = new ShadowField<>(DEFAULT_MAIN_VOLUME,
			number -> new SimpleFloatProperty(number.floatValue()));

	public FloatProperty mainVolumeProperty() {
		return mainVolume.property();
	}

	@Override
	public float getMainVolume() {
		return this.mainVolume.get().floatValue();
	}

	@Override
	public void setMainVolume(float volume) {
		this.mainVolume.set(volume);
	}

	private ShadowField<FloatProperty, Number> secondVolume = new ShadowField<>(DEFAULT_SECOND_VOLUME,
			number -> new SimpleFloatProperty(number.floatValue()));

	public FloatProperty secondVolumeProperty() {
		return secondVolume.property();
	}

	@Override
	public float getSecondVolume() {
		return this.secondVolume.get().floatValue();
	}

	@Override
	public void setSecondVolume(float volume) {
		this.secondVolume.set(volume);
	}

	private ShadowField<FloatProperty, Number> thirdVolume = new ShadowField<>(DEFAULT_THIRD_VOLUME,
			number -> new SimpleFloatProperty(number.floatValue()));

	public FloatProperty thirdVolumeProperty() {
		return thirdVolume.property();
	}

	@Override
	public float getThirdVolume() {
		return this.thirdVolume.get().floatValue();
	}

	@Override
	public void setThirdVolume(float volume) {
		this.thirdVolume.set(volume);
	}

	private ShadowField<FloatProperty, Number> mainBalance = new ShadowField<>(DEFAULT_MAIN_BALANCE,
			number -> new SimpleFloatProperty(number.floatValue()));

	public FloatProperty mainBalanceProperty() {
		return mainBalance.property();
	}

	@Override
	public float getMainBalance() {
		return this.mainBalance.get().floatValue();
	}

	@Override
	public void setMainBalance(float balance) {
		this.mainBalance.set(balance);
	}

	private ShadowField<FloatProperty, Number> secondBalance = new ShadowField<>(DEFAULT_SECOND_BALANCE,
			number -> new SimpleFloatProperty(number.floatValue()));

	public FloatProperty secondBalanceProperty() {
		return secondBalance.property();
	}

	@Override
	public float getSecondBalance() {
		return this.secondBalance.get().floatValue();
	}

	@Override
	public void setSecondBalance(float right) {
		this.secondBalance.set(right);
	}

	private ShadowField<FloatProperty, Number> thirdBalance = new ShadowField<>(DEFAULT_THIRD_BALANCE,
			number -> new SimpleFloatProperty(number.floatValue()));

	public FloatProperty thirdBalanceProperty() {
		return thirdBalance.property();
	}

	@Override
	public float getThirdBalance() {
		return this.thirdBalance.get().floatValue();
	}

	@Override
	public void setThirdBalance(float third) {
		this.thirdBalance.set(third);
	}

	private ShadowField<IntegerProperty, Number> mainDelay = new ShadowField<>(DEFAULT_MAIN_DELAY,
			number -> new SimpleIntegerProperty(number.intValue()));

	public IntegerProperty mainDelayProperty() {
		return mainDelay.property();
	}

	@Override
	public int getMainDelay() {
		return this.mainDelay.get().intValue();
	}

	@Override
	public void setMainDelay(int delay) {
		this.mainDelay.set(delay);
	}

	private ShadowField<IntegerProperty, Number> secondDelay = new ShadowField<>(DEFAULT_SECOND_DELAY,
			number -> new SimpleIntegerProperty(number.intValue()));

	public IntegerProperty secondDelayProperty() {
		return secondDelay.property();
	}

	@Override
	public int getSecondDelay() {
		return this.secondDelay.get().intValue();
	}

	@Override
	public void setSecondDelay(int delay) {
		this.secondDelay.set(delay);
	}

	private ShadowField<IntegerProperty, Number> thirdDelay = new ShadowField<>(DEFAULT_THIRD_DELAY,
			number -> new SimpleIntegerProperty(number.intValue()));

	public IntegerProperty thirdDelayProperty() {
		return this.thirdDelay.property();
	}

	@Override
	public int getThirdDelay() {
		return this.thirdDelay.get().intValue();
	}

	@Override
	public void setThirdDelay(int delay) {
		this.thirdDelay.set(delay);
	}

	private ShadowField<IntegerProperty, Number> bufferSize = new ShadowField<>(DEFAULT_BUFFER_SIZE,
			number -> new SimpleIntegerProperty(number.intValue()));

	public IntegerProperty bufferSizeProperty() {
		return bufferSize.property();
	}

	@Override
	public int getBufferSize() {
		return bufferSize.get().intValue();
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize.set(bufferSize);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> audioBufferSize = new ShadowField<>(DEFAULT_AUDIO_BUFFER_SIZE,
			SimpleObjectProperty::new);

	public ObjectProperty<Integer> audioBufferSizeProperty() {
		return audioBufferSize.property();
	}

	@Override
	public int getAudioBufferSize() {
		return audioBufferSize.get();
	}

	@Override
	public void setAudioBufferSize(int audioBufferSize) {
		this.audioBufferSize.set(audioBufferSize);
	}

	private ShadowField<BooleanProperty, Boolean> playOriginal = new ShadowField<>(DEFAULT_PLAY_ORIGINAL,
			SimpleBooleanProperty::new);

	@Override
	public boolean isPlayOriginal() {
		return playOriginal.get();
	}

	@Override
	public void setPlayOriginal(boolean original) {
		this.playOriginal.set(original);
	}

	public BooleanProperty playOriginalProperty() {
		return playOriginal.property();
	}

	private ShadowField<ObjectProperty<File>, File> mp3File = new ShadowField<>(null, SimpleObjectProperty::new);

	public ObjectProperty<File> mp3FileProperty() {
		return mp3File.property();
	}

	private ShadowField<ObjectProperty<String>, String> mp3 = new ShadowField<>(null, SimpleObjectProperty::new);

	@Override
	public String getMp3File() {
		return this.mp3.get();
	}

	@Override
	public void setMp3File(String recording) {
		this.mp3.set(recording);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> cbr = new ShadowField<>(DEFAULT_CBR,
			SimpleObjectProperty::new);

	@Override
	public int getCbr() {
		return cbr.get();
	}

	@Override
	public void setCbr(int cbr) {
		this.cbr.set(cbr);
	}

	public ObjectProperty<Integer> cbrProperty() {
		return cbr.property();
	}

	private ShadowField<BooleanProperty, Boolean> vbr = new ShadowField<>(DEFAULT_VBR, SimpleBooleanProperty::new);

	@Override
	public boolean isVbr() {
		return vbr.get();
	}

	@Override
	public void setVbr(boolean vbr) {
		this.vbr.set(vbr);
	}

	public BooleanProperty vbrProperty() {
		return vbr.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> vbrQuality = new ShadowField<>(DEFAULT_VBR_QUALITY,
			SimpleObjectProperty::new);

	@Override
	public int getVbrQuality() {
		return vbrQuality.get();
	}

	@Override
	public void setVbrQuality(int vbrQuality) {
		this.vbrQuality.set(vbrQuality);
	}

	public ObjectProperty<Integer> vbrQualityProperty() {
		return vbrQuality.property();
	}

	private ShadowField<FloatProperty, Number> aviCompressionQuality = new ShadowField<>(
			DEFAULT_AVI_COMPRESSION_QUALITY, number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getAviCompressionQuality() {
		return aviCompressionQuality.get().floatValue();
	}

	@Override
	public void setAviCompressionQuality(float aviCompressionQuality) {
		this.aviCompressionQuality.set(aviCompressionQuality);
	}

	public FloatProperty aviCompressionQualityProperty() {
		return aviCompressionQuality.property();
	}

	private ShadowField<BooleanProperty, Boolean> delayBypass = new ShadowField<>(DEFAULT_DELAY_BYPASS,
			SimpleBooleanProperty::new);

	@Override
	public boolean getDelayBypass() {
		return delayBypass.get();
	}

	@Override
	public void setDelayBypass(boolean delayBypass) {
		this.delayBypass.set(delayBypass);
	}

	public BooleanProperty delayBypassProperty() {
		return delayBypass.property();
	}

	private ShadowField<IntegerProperty, Number> delay = new ShadowField<>(DEFAULT_DELAY,
			number -> new SimpleIntegerProperty(number.intValue()));

	@Override
	public int getDelay() {
		return delay.get().intValue();
	}

	@Override
	public void setDelay(int delay) {
		this.delay.set(delay);
	}

	public IntegerProperty delayProperty() {
		return delay.property();
	}

	private ShadowField<IntegerProperty, Number> delayDryLevel = new ShadowField<>(DEFAULT_DELAY_DRY_LEVEL,
			number -> new SimpleIntegerProperty(number.intValue()));

	@Override
	public int getDelayDryLevel() {
		return delayDryLevel.get().intValue();
	}

	@Override
	public void setDelayDryLevel(int delayDryLevel) {
		this.delayDryLevel.set(delayDryLevel);
	}

	public IntegerProperty delayDryLevelProperty() {
		return delayDryLevel.property();
	}

	private ShadowField<IntegerProperty, Number> delayWetLevel = new ShadowField<>(DEFAULT_DELAY_WET_LEVEL,
			number -> new SimpleIntegerProperty(number.intValue()));

	@Override
	public int getDelayWetLevel() {
		return delayWetLevel.get().intValue();
	}

	@Override
	public void setDelayWetLevel(int delayWetLevel) {
		this.delayWetLevel.set(delayWetLevel);
	}

	public IntegerProperty delayWetLevelProperty() {
		return delayWetLevel.property();
	}

	private ShadowField<IntegerProperty, Number> delayFeedbackLevel = new ShadowField<>(DEFAULT_DELAY_FEEDBACK_LEVEL,
			number -> new SimpleIntegerProperty(number.intValue()));

	@Override
	public int getDelayFeedbackLevel() {
		return delayFeedbackLevel.get().intValue();
	}

	@Override
	public void setDelayFeedbackLevel(int delayFeedbackLevel) {
		this.delayFeedbackLevel.set(delayFeedbackLevel);
	}

	public IntegerProperty delayFeedbackLevelProperty() {
		return delayFeedbackLevel.property();
	}

	private ShadowField<BooleanProperty, Boolean> reverbBypass = new ShadowField<>(DEFAULT_REVERB_BYPASS,
			SimpleBooleanProperty::new);

	@Override
	public boolean getReverbBypass() {
		return reverbBypass.get();
	}

	@Override
	public void setReverbBypass(boolean reverbBypass) {
		this.reverbBypass.set(reverbBypass);
	}

	public BooleanProperty reverbBypassProperty() {
		return reverbBypass.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb1Delay = new ShadowField<>(DEFAULT_REVERB_COMB1_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbComb1Delay() {
		return reverbComb1Delay.get().floatValue();
	}

	@Override
	public void setReverbComb1Delay(float reverbComb1Delay) {
		this.reverbComb1Delay.set(reverbComb1Delay);
	}

	public FloatProperty reverbComb1DelayProperty() {
		return reverbComb1Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb2Delay = new ShadowField<>(DEFAULT_REVERB_COMB2_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbComb2Delay() {
		return reverbComb2Delay.get().floatValue();
	}

	@Override
	public void setReverbComb2Delay(float reverbComb2Delay) {
		this.reverbComb2Delay.set(reverbComb2Delay);
	}

	public FloatProperty reverbComb2DelayProperty() {
		return reverbComb2Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb3Delay = new ShadowField<>(DEFAULT_REVERB_COMB3_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbComb3Delay() {
		return reverbComb3Delay.get().floatValue();
	}

	@Override
	public void setReverbComb3Delay(float reverbComb3Delay) {
		this.reverbComb3Delay.set(reverbComb3Delay);
	}

	public FloatProperty reverbComb3DelayProperty() {
		return reverbComb3Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb4Delay = new ShadowField<>(DEFAULT_REVERB_COMB4_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbComb4Delay() {
		return reverbComb4Delay.get().floatValue();
	}

	@Override
	public void setReverbComb4Delay(float reverbComb4Delay) {
		this.reverbComb4Delay.set(reverbComb4Delay);
	}

	public FloatProperty reverbComb4DelayProperty() {
		return reverbComb4Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbAllPass1Delay = new ShadowField<>(DEFAULT_REVERB_ALL_PASS1_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbAllPass1Delay() {
		return reverbAllPass1Delay.get().floatValue();
	}

	@Override
	public void setReverbAllPass1Delay(float reverbAllPass1Delay) {
		this.reverbAllPass1Delay.set(reverbAllPass1Delay);
	}

	public FloatProperty reverbAllPass1DelayProperty() {
		return reverbAllPass1Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbAllPass2Delay = new ShadowField<>(DEFAULT_REVERB_ALL_PASS2_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbAllPass2Delay() {
		return reverbAllPass2Delay.get().floatValue();
	}

	@Override
	public void setReverbAllPass2Delay(float reverbAllPass2Delay) {
		this.reverbAllPass2Delay.set(reverbAllPass2Delay);
	}

	public FloatProperty reverbAllPass2DelayProperty() {
		return reverbAllPass2Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbSustainDelay = new ShadowField<>(DEFAULT_REVERB_SUSTAIN_DELAY,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbSustainDelay() {
		return reverbSustainDelay.get().floatValue();
	}

	@Override
	public void setReverbSustainDelay(float reverbSustainDelay) {
		this.reverbSustainDelay.set(reverbSustainDelay);
	}

	public FloatProperty reverbSustainDelayProperty() {
		return reverbSustainDelay.property();
	}

	private ShadowField<FloatProperty, Number> reverbDryWetMix = new ShadowField<>(DEFAULT_REVERB_DRY_WET_MIX,
			number -> new SimpleFloatProperty(number.floatValue()));

	@Override
	public float getReverbDryWetMix() {
		return reverbDryWetMix.get().floatValue();
	}

	@Override
	public void setReverbDryWetMix(float reverbDryWetMix) {
		this.reverbDryWetMix.set(reverbDryWetMix);
	}

	public FloatProperty reverbDryWetMixProperty() {
		return reverbDryWetMix.property();
	}

}
