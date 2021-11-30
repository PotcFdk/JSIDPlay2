package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_CODER_BIT_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_CBR;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_DRY_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_FEEDBACK_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_WET_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_EXSID_FAKE_STEREO;
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
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_BIT_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_GLOBAL_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_GOP;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_PRESET;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_STREAMING_URL;

import java.io.File;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
import libsidplay.common.VideoCoderPreset;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.converter.FileAttributeConverter;
import ui.common.converter.FileToStringDeserializer;
import ui.common.converter.FileToStringSerializer;
import ui.common.converter.FileXmlAdapter;
import ui.common.properties.ShadowField;

@Embeddable
public class AudioSection implements IAudioSection {

	private ShadowField<ObjectProperty<Audio>, Audio> audio = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_AUDIO);

	@Enumerated(EnumType.STRING)
	@Override
	public final Audio getAudio() {
		return audio.get();
	}

	@Override
	public final void setAudio(Audio audio) {
		this.audio.set(audio);
	}

	public final ObjectProperty<Audio> audioProperty() {
		return audio.property();
	}

	private ShadowField<IntegerProperty, Number> device = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DEVICE);

	@Override
	public final int getDevice() {
		return device.get().intValue();
	}

	@Override
	public final void setDevice(int device) {
		this.device.set(device);
	}

	public final IntegerProperty deviceProperty() {
		return device.property();
	}

	private ShadowField<ObjectProperty<SamplingRate>, SamplingRate> samplingRate = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SAMPLING_RATE);

	@Enumerated(EnumType.STRING)
	@Override
	public final SamplingRate getSamplingRate() {
		return this.samplingRate.get();
	}

	@Override
	public final void setSamplingRate(SamplingRate samplingRate) {
		this.samplingRate.set(samplingRate);
	}

	public final ObjectProperty<SamplingRate> samplingRateProperty() {
		return samplingRate.property();
	}

	private ShadowField<ObjectProperty<SamplingMethod>, SamplingMethod> sampling = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SAMPLING);

	@Enumerated(EnumType.STRING)
	@Override
	public final SamplingMethod getSampling() {
		return this.sampling.get();
	}

	@Override
	public final void setSampling(SamplingMethod method) {
		this.sampling.set(method);
	}

	public final ObjectProperty<SamplingMethod> samplingProperty() {
		return sampling.property();
	}

	private ShadowField<FloatProperty, Number> mainVolume = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_MAIN_VOLUME);

	@Override
	public final float getMainVolume() {
		return this.mainVolume.get().floatValue();
	}

	@Override
	public final void setMainVolume(float volume) {
		this.mainVolume.set(volume);
	}

	public final FloatProperty mainVolumeProperty() {
		return mainVolume.property();
	}

	private ShadowField<FloatProperty, Number> secondVolume = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SECOND_VOLUME);

	@Override
	public final float getSecondVolume() {
		return this.secondVolume.get().floatValue();
	}

	@Override
	public final void setSecondVolume(float volume) {
		this.secondVolume.set(volume);
	}

	public final FloatProperty secondVolumeProperty() {
		return secondVolume.property();
	}

	private ShadowField<FloatProperty, Number> thirdVolume = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_THIRD_VOLUME);

	@Override
	public final float getThirdVolume() {
		return this.thirdVolume.get().floatValue();
	}

	@Override
	public final void setThirdVolume(float volume) {
		this.thirdVolume.set(volume);
	}

	public final FloatProperty thirdVolumeProperty() {
		return thirdVolume.property();
	}

	private ShadowField<FloatProperty, Number> mainBalance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_MAIN_BALANCE);

	@Override
	public final float getMainBalance() {
		return this.mainBalance.get().floatValue();
	}

	@Override
	public final void setMainBalance(float balance) {
		this.mainBalance.set(balance);
	}

	public final FloatProperty mainBalanceProperty() {
		return mainBalance.property();
	}

	private ShadowField<FloatProperty, Number> secondBalance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SECOND_BALANCE);

	@Override
	public final float getSecondBalance() {
		return this.secondBalance.get().floatValue();
	}

	@Override
	public final void setSecondBalance(float right) {
		this.secondBalance.set(right);
	}

	public final FloatProperty secondBalanceProperty() {
		return secondBalance.property();
	}

	private ShadowField<FloatProperty, Number> thirdBalance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_THIRD_BALANCE);

	@Override
	public final float getThirdBalance() {
		return this.thirdBalance.get().floatValue();
	}

	@Override
	public final void setThirdBalance(float third) {
		this.thirdBalance.set(third);
	}

	public final FloatProperty thirdBalanceProperty() {
		return thirdBalance.property();
	}

	private ShadowField<IntegerProperty, Number> mainDelay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_MAIN_DELAY);

	@Override
	public final int getMainDelay() {
		return this.mainDelay.get().intValue();
	}

	@Override
	public final void setMainDelay(int delay) {
		this.mainDelay.set(delay);
	}

	public final IntegerProperty mainDelayProperty() {
		return mainDelay.property();
	}

	private ShadowField<IntegerProperty, Number> secondDelay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_SECOND_DELAY);

	@Override
	public final int getSecondDelay() {
		return this.secondDelay.get().intValue();
	}

	@Override
	public final void setSecondDelay(int delay) {
		this.secondDelay.set(delay);
	}

	public final IntegerProperty secondDelayProperty() {
		return secondDelay.property();
	}

	private ShadowField<IntegerProperty, Number> thirdDelay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_THIRD_DELAY);

	@Override
	public final int getThirdDelay() {
		return this.thirdDelay.get().intValue();
	}

	@Override
	public final void setThirdDelay(int delay) {
		this.thirdDelay.set(delay);
	}

	public final IntegerProperty thirdDelayProperty() {
		return this.thirdDelay.property();
	}

	private ShadowField<IntegerProperty, Number> bufferSize = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_BUFFER_SIZE);

	@Override
	public final int getBufferSize() {
		return bufferSize.get().intValue();
	}

	@Override
	public final void setBufferSize(int bufferSize) {
		this.bufferSize.set(bufferSize);
	}

	public final IntegerProperty bufferSizeProperty() {
		return bufferSize.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> audioBufferSize = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_AUDIO_BUFFER_SIZE);

	@Override
	public final int getAudioBufferSize() {
		return audioBufferSize.get();
	}

	@Override
	public final void setAudioBufferSize(int audioBufferSize) {
		this.audioBufferSize.set(audioBufferSize);
	}

	public final ObjectProperty<Integer> audioBufferSizeProperty() {
		return audioBufferSize.property();
	}

	private ShadowField<BooleanProperty, Boolean> playOriginal = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PLAY_ORIGINAL);

	@Override
	public final boolean isPlayOriginal() {
		return playOriginal.get();
	}

	@Override
	public final void setPlayOriginal(boolean original) {
		this.playOriginal.set(original);
	}

	public final BooleanProperty playOriginalProperty() {
		return playOriginal.property();
	}

	private ShadowField<ObjectProperty<File>, File> mp3 = new ShadowField<>(SimpleObjectProperty::new, null);

	@Override
	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getMp3() {
		return this.mp3.get();
	}

	@Override
	public final void setMp3(File recording) {
		this.mp3.set(recording);
	}

	public final ObjectProperty<File> mp3Property() {
		return mp3.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> cbr = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_CBR);

	@Override
	public final int getCbr() {
		return cbr.get();
	}

	@Override
	public final void setCbr(int cbr) {
		this.cbr.set(cbr);
	}

	public final ObjectProperty<Integer> cbrProperty() {
		return cbr.property();
	}

	private ShadowField<BooleanProperty, Boolean> vbr = new ShadowField<>(SimpleBooleanProperty::new, DEFAULT_VBR);

	@Override
	public final boolean isVbr() {
		return vbr.get();
	}

	@Override
	public final void setVbr(boolean vbr) {
		this.vbr.set(vbr);
	}

	public final BooleanProperty vbrProperty() {
		return vbr.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> vbrQuality = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_VBR_QUALITY);

	@Override
	public final int getVbrQuality() {
		return vbrQuality.get();
	}

	@Override
	public final void setVbrQuality(int vbrQuality) {
		this.vbrQuality.set(vbrQuality);
	}

	public final ObjectProperty<Integer> vbrQualityProperty() {
		return vbrQuality.property();
	}

	private ShadowField<BooleanProperty, Boolean> delayBypass = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DELAY_BYPASS);

	@Override
	public final boolean getDelayBypass() {
		return delayBypass.get();
	}

	@Override
	public final void setDelayBypass(boolean delayBypass) {
		this.delayBypass.set(delayBypass);
	}

	public final BooleanProperty delayBypassProperty() {
		return delayBypass.property();
	}

	private ShadowField<IntegerProperty, Number> audioCoderBitRate = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_AUDIO_CODER_BIT_RATE);

	@Override
	public final int getAudioCoderBitRate() {
		return audioCoderBitRate.get().intValue();
	}

	@Override
	public final void setAudioCoderBitRate(int bitRate) {
		this.audioCoderBitRate.set(bitRate);
	}

	public final IntegerProperty audioCoderBitRateProperty() {
		return audioCoderBitRate.property();
	}

	private ShadowField<IntegerProperty, Number> audioCoderBitRateTolerance = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE);

	@Override
	public final int getAudioCoderBitRateTolerance() {
		return audioCoderBitRateTolerance.get().intValue();
	}

	@Override
	public final void setAudioCoderBitRateTolerance(int bitRateTolerance) {
		this.audioCoderBitRateTolerance.set(bitRateTolerance);
	}

	public final IntegerProperty audioCoderBitRateToleranceProperty() {
		return audioCoderBitRateTolerance.property();
	}

	private ShadowField<StringProperty, String> videoStreamingUrl = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_VIDEO_STREAMING_URL);

	@Override
	public final String getVideoStreamingUrl() {
		return videoStreamingUrl.get();
	}

	@Override
	public final void setVideoStreamingUrl(String videoStreamingUrl) {
		this.videoStreamingUrl.set(videoStreamingUrl);
	}

	public final StringProperty videoStreamingUrlProperty() {
		return videoStreamingUrl.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderNumPicturesInGroupOfPictures = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_GOP);

	@Override
	public final int getVideoCoderNumPicturesInGroupOfPictures() {
		return videoCoderNumPicturesInGroupOfPictures.get().intValue();
	}

	@Override
	public final void setVideoCoderNumPicturesInGroupOfPictures(int numPicturesInGroupOfPictures) {
		this.videoCoderNumPicturesInGroupOfPictures.set(numPicturesInGroupOfPictures);
	}

	public final IntegerProperty videoCoderNumPicturesInGroupOfPicturesProperty() {
		return videoCoderNumPicturesInGroupOfPictures.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderBitRate = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_BIT_RATE);

	@Override
	public final int getVideoCoderBitRate() {
		return videoCoderBitRate.get().intValue();
	}

	@Override
	public final void setVideoCoderBitRate(int bitRate) {
		this.videoCoderBitRate.set(bitRate);
	}

	public final IntegerProperty videoCoderBitRateProperty() {
		return videoCoderBitRate.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderBitRateTolerance = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE);

	@Override
	public final int getVideoCoderBitRateTolerance() {
		return videoCoderBitRateTolerance.get().intValue();
	}

	@Override
	public final void setVideoCoderBitRateTolerance(int bitRateTolerance) {
		this.videoCoderBitRateTolerance.set(bitRateTolerance);
	}

	public final IntegerProperty videoCoderBitRateToleranceProperty() {
		return videoCoderBitRateTolerance.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderGlobalQuality = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_GLOBAL_QUALITY);

	@Override
	public final int getVideoCoderGlobalQuality() {
		return videoCoderGlobalQuality.get().intValue();
	}

	@Override
	public final void setVideoCoderGlobalQuality(int globalQuality) {
		this.videoCoderGlobalQuality.set(globalQuality);
	}

	public final IntegerProperty videoCoderGlobalQualityProperty() {
		return videoCoderGlobalQuality.property();
	}

	private ShadowField<ObjectProperty<VideoCoderPreset>, VideoCoderPreset> videoCoderPreset = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_VIDEO_CODER_PRESET);

	@Enumerated(EnumType.STRING)
	@Override
	public final VideoCoderPreset getVideoCoderPreset() {
		return videoCoderPreset.get();
	}

	@Override
	public final void setVideoCoderPreset(VideoCoderPreset videoEncoderPreset) {
		this.videoCoderPreset.set(videoEncoderPreset);
	}

	public final ObjectProperty<VideoCoderPreset> sidVideoEncoderPresetProperty() {
		return videoCoderPreset.property();
	}

	private ShadowField<IntegerProperty, Number> delay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY);

	@Override
	public final int getDelay() {
		return delay.get().intValue();
	}

	@Override
	public final void setDelay(int delay) {
		this.delay.set(delay);
	}

	public final IntegerProperty delayProperty() {
		return delay.property();
	}

	private ShadowField<IntegerProperty, Number> delayDryLevel = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY_DRY_LEVEL);

	@Override
	public final int getDelayDryLevel() {
		return delayDryLevel.get().intValue();
	}

	@Override
	public final void setDelayDryLevel(int delayDryLevel) {
		this.delayDryLevel.set(delayDryLevel);
	}

	public final IntegerProperty delayDryLevelProperty() {
		return delayDryLevel.property();
	}

	private ShadowField<IntegerProperty, Number> delayWetLevel = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY_WET_LEVEL);

	@Override
	public final int getDelayWetLevel() {
		return delayWetLevel.get().intValue();
	}

	@Override
	public final void setDelayWetLevel(int delayWetLevel) {
		this.delayWetLevel.set(delayWetLevel);
	}

	public final IntegerProperty delayWetLevelProperty() {
		return delayWetLevel.property();
	}

	private ShadowField<IntegerProperty, Number> delayFeedbackLevel = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY_FEEDBACK_LEVEL);

	@Override
	public final int getDelayFeedbackLevel() {
		return delayFeedbackLevel.get().intValue();
	}

	@Override
	public final void setDelayFeedbackLevel(int delayFeedbackLevel) {
		this.delayFeedbackLevel.set(delayFeedbackLevel);
	}

	public final IntegerProperty delayFeedbackLevelProperty() {
		return delayFeedbackLevel.property();
	}

	private ShadowField<BooleanProperty, Boolean> reverbBypass = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_REVERB_BYPASS);

	@Override
	public final boolean getReverbBypass() {
		return reverbBypass.get();
	}

	@Override
	public final void setReverbBypass(boolean reverbBypass) {
		this.reverbBypass.set(reverbBypass);
	}

	public final BooleanProperty reverbBypassProperty() {
		return reverbBypass.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb1Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB1_DELAY);

	@Override
	public final float getReverbComb1Delay() {
		return reverbComb1Delay.get().floatValue();
	}

	@Override
	public final void setReverbComb1Delay(float reverbComb1Delay) {
		this.reverbComb1Delay.set(reverbComb1Delay);
	}

	public final FloatProperty reverbComb1DelayProperty() {
		return reverbComb1Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb2Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB2_DELAY);

	@Override
	public final float getReverbComb2Delay() {
		return reverbComb2Delay.get().floatValue();
	}

	@Override
	public final void setReverbComb2Delay(float reverbComb2Delay) {
		this.reverbComb2Delay.set(reverbComb2Delay);
	}

	public final FloatProperty reverbComb2DelayProperty() {
		return reverbComb2Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb3Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB3_DELAY);

	@Override
	public final float getReverbComb3Delay() {
		return reverbComb3Delay.get().floatValue();
	}

	@Override
	public final void setReverbComb3Delay(float reverbComb3Delay) {
		this.reverbComb3Delay.set(reverbComb3Delay);
	}

	public final FloatProperty reverbComb3DelayProperty() {
		return reverbComb3Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbComb4Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB4_DELAY);

	@Override
	public final float getReverbComb4Delay() {
		return reverbComb4Delay.get().floatValue();
	}

	@Override
	public final void setReverbComb4Delay(float reverbComb4Delay) {
		this.reverbComb4Delay.set(reverbComb4Delay);
	}

	public final FloatProperty reverbComb4DelayProperty() {
		return reverbComb4Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbAllPass1Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_ALL_PASS1_DELAY);

	@Override
	public final float getReverbAllPass1Delay() {
		return reverbAllPass1Delay.get().floatValue();
	}

	@Override
	public final void setReverbAllPass1Delay(float reverbAllPass1Delay) {
		this.reverbAllPass1Delay.set(reverbAllPass1Delay);
	}

	public final FloatProperty reverbAllPass1DelayProperty() {
		return reverbAllPass1Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbAllPass2Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_ALL_PASS2_DELAY);

	@Override
	public final float getReverbAllPass2Delay() {
		return reverbAllPass2Delay.get().floatValue();
	}

	@Override
	public final void setReverbAllPass2Delay(float reverbAllPass2Delay) {
		this.reverbAllPass2Delay.set(reverbAllPass2Delay);
	}

	public final FloatProperty reverbAllPass2DelayProperty() {
		return reverbAllPass2Delay.property();
	}

	private ShadowField<FloatProperty, Number> reverbSustainDelay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_SUSTAIN_DELAY);

	@Override
	public final float getReverbSustainDelay() {
		return reverbSustainDelay.get().floatValue();
	}

	@Override
	public final void setReverbSustainDelay(float reverbSustainDelay) {
		this.reverbSustainDelay.set(reverbSustainDelay);
	}

	public final FloatProperty reverbSustainDelayProperty() {
		return reverbSustainDelay.property();
	}

	private ShadowField<FloatProperty, Number> reverbDryWetMix = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_DRY_WET_MIX);

	@Override
	public final float getReverbDryWetMix() {
		return reverbDryWetMix.get().floatValue();
	}

	@Override
	public final void setReverbDryWetMix(float reverbDryWetMix) {
		this.reverbDryWetMix.set(reverbDryWetMix);
	}

	public final FloatProperty reverbDryWetMixProperty() {
		return reverbDryWetMix.property();
	}

	private ShadowField<BooleanProperty, Boolean> exsidFakeStereo = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_EXSID_FAKE_STEREO);

	@Override
	public final boolean isExsidFakeStereo() {
		return exsidFakeStereo.get();
	}

	@Override
	public final void setExsidFakeStereo(boolean exsidFakeStereo) {
		this.exsidFakeStereo.set(exsidFakeStereo);
	}

	public final BooleanProperty exsidFakeStereoProperty() {
		return exsidFakeStereo.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
