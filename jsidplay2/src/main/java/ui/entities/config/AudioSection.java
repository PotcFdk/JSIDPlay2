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

	private ShadowField<IntegerProperty, Number> device = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DEVICE);

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
			SimpleObjectProperty::new, DEFAULT_SAMPLING_RATE);

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

	private ShadowField<ObjectProperty<SamplingMethod>, SamplingMethod> sampling = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SAMPLING);

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

	private ShadowField<FloatProperty, Number> mainVolume = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_MAIN_VOLUME);

	@Override
	public float getMainVolume() {
		return this.mainVolume.get().floatValue();
	}

	@Override
	public void setMainVolume(float volume) {
		this.mainVolume.set(volume);
	}

	public FloatProperty mainVolumeProperty() {
		return mainVolume.property();
	}

	private ShadowField<FloatProperty, Number> secondVolume = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SECOND_VOLUME);

	@Override
	public float getSecondVolume() {
		return this.secondVolume.get().floatValue();
	}

	@Override
	public void setSecondVolume(float volume) {
		this.secondVolume.set(volume);
	}

	public FloatProperty secondVolumeProperty() {
		return secondVolume.property();
	}

	private ShadowField<FloatProperty, Number> thirdVolume = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_THIRD_VOLUME);

	@Override
	public float getThirdVolume() {
		return this.thirdVolume.get().floatValue();
	}

	@Override
	public void setThirdVolume(float volume) {
		this.thirdVolume.set(volume);
	}

	public FloatProperty thirdVolumeProperty() {
		return thirdVolume.property();
	}

	private ShadowField<FloatProperty, Number> mainBalance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_MAIN_BALANCE);

	@Override
	public float getMainBalance() {
		return this.mainBalance.get().floatValue();
	}

	@Override
	public void setMainBalance(float balance) {
		this.mainBalance.set(balance);
	}

	public FloatProperty mainBalanceProperty() {
		return mainBalance.property();
	}

	private ShadowField<FloatProperty, Number> secondBalance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SECOND_BALANCE);

	@Override
	public float getSecondBalance() {
		return this.secondBalance.get().floatValue();
	}

	@Override
	public void setSecondBalance(float right) {
		this.secondBalance.set(right);
	}

	public FloatProperty secondBalanceProperty() {
		return secondBalance.property();
	}

	private ShadowField<FloatProperty, Number> thirdBalance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_THIRD_BALANCE);

	@Override
	public float getThirdBalance() {
		return this.thirdBalance.get().floatValue();
	}

	@Override
	public void setThirdBalance(float third) {
		this.thirdBalance.set(third);
	}

	public FloatProperty thirdBalanceProperty() {
		return thirdBalance.property();
	}

	private ShadowField<IntegerProperty, Number> mainDelay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_MAIN_DELAY);

	@Override
	public int getMainDelay() {
		return this.mainDelay.get().intValue();
	}

	@Override
	public void setMainDelay(int delay) {
		this.mainDelay.set(delay);
	}

	public IntegerProperty mainDelayProperty() {
		return mainDelay.property();
	}

	private ShadowField<IntegerProperty, Number> secondDelay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_SECOND_DELAY);

	@Override
	public int getSecondDelay() {
		return this.secondDelay.get().intValue();
	}

	@Override
	public void setSecondDelay(int delay) {
		this.secondDelay.set(delay);
	}

	public IntegerProperty secondDelayProperty() {
		return secondDelay.property();
	}

	private ShadowField<IntegerProperty, Number> thirdDelay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_THIRD_DELAY);

	@Override
	public int getThirdDelay() {
		return this.thirdDelay.get().intValue();
	}

	@Override
	public void setThirdDelay(int delay) {
		this.thirdDelay.set(delay);
	}

	public IntegerProperty thirdDelayProperty() {
		return this.thirdDelay.property();
	}

	private ShadowField<IntegerProperty, Number> bufferSize = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_BUFFER_SIZE);

	@Override
	public int getBufferSize() {
		return bufferSize.get().intValue();
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize.set(bufferSize);
	}

	public IntegerProperty bufferSizeProperty() {
		return bufferSize.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> audioBufferSize = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_AUDIO_BUFFER_SIZE);

	@Override
	public int getAudioBufferSize() {
		return audioBufferSize.get();
	}

	@Override
	public void setAudioBufferSize(int audioBufferSize) {
		this.audioBufferSize.set(audioBufferSize);
	}

	public ObjectProperty<Integer> audioBufferSizeProperty() {
		return audioBufferSize.property();
	}

	private ShadowField<BooleanProperty, Boolean> playOriginal = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PLAY_ORIGINAL);

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

	private ShadowField<ObjectProperty<File>, File> mp3 = new ShadowField<>(SimpleObjectProperty::new, null);

	@Override
	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public File getMp3() {
		return this.mp3.get();
	}

	@Override
	public void setMp3(File recording) {
		this.mp3.set(recording);
	}

	public ObjectProperty<File> mp3Property() {
		return mp3.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> cbr = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_CBR);

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

	private ShadowField<BooleanProperty, Boolean> vbr = new ShadowField<>(SimpleBooleanProperty::new, DEFAULT_VBR);

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

	private ShadowField<ObjectProperty<Integer>, Integer> vbrQuality = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_VBR_QUALITY);

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

	private ShadowField<BooleanProperty, Boolean> delayBypass = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DELAY_BYPASS);

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

	private ShadowField<IntegerProperty, Number> audioCoderBitRate = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_AUDIO_CODER_BIT_RATE);

	@Override
	public int getAudioCoderBitRate() {
		return audioCoderBitRate.get().intValue();
	}

	@Override
	public void setAudioCoderBitRate(int bitRate) {
		this.audioCoderBitRate.set(bitRate);
	}

	public IntegerProperty audioCoderBitRateProperty() {
		return audioCoderBitRate.property();
	}

	private ShadowField<IntegerProperty, Number> audioCoderBitRateTolerance = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE);

	@Override
	public int getAudioCoderBitRateTolerance() {
		return audioCoderBitRateTolerance.get().intValue();
	}

	@Override
	public void setAudioCoderBitRateTolerance(int bitRateTolerance) {
		this.audioCoderBitRateTolerance.set(bitRateTolerance);
	}

	public IntegerProperty audioCoderBitRateToleranceProperty() {
		return audioCoderBitRateTolerance.property();
	}

	private ShadowField<StringProperty, String> videoStreamingUrl = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_VIDEO_STREAMING_URL);

	@Override
	public String getVideoStreamingUrl() {
		return videoStreamingUrl.get();
	}

	@Override
	public void setVideoStreamingUrl(String videoStreamingUrl) {
		this.videoStreamingUrl.set(videoStreamingUrl);
	}

	public StringProperty videoStreamingUrlProperty() {
		return videoStreamingUrl.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderNumPicturesInGroupOfPictures = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_GOP);

	@Override
	public int getVideoCoderNumPicturesInGroupOfPictures() {
		return videoCoderNumPicturesInGroupOfPictures.get().intValue();
	}

	@Override
	public void setVideoCoderNumPicturesInGroupOfPictures(int numPicturesInGroupOfPictures) {
		this.videoCoderNumPicturesInGroupOfPictures.set(numPicturesInGroupOfPictures);
	}

	public IntegerProperty videoCoderNumPicturesInGroupOfPicturesProperty() {
		return videoCoderNumPicturesInGroupOfPictures.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderBitRate = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_BIT_RATE);

	@Override
	public int getVideoCoderBitRate() {
		return videoCoderBitRate.get().intValue();
	}

	@Override
	public void setVideoCoderBitRate(int bitRate) {
		this.videoCoderBitRate.set(bitRate);
	}

	public IntegerProperty videoCoderBitRateProperty() {
		return videoCoderBitRate.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderBitRateTolerance = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE);

	@Override
	public int getVideoCoderBitRateTolerance() {
		return videoCoderBitRateTolerance.get().intValue();
	}

	@Override
	public void setVideoCoderBitRateTolerance(int bitRateTolerance) {
		this.videoCoderBitRateTolerance.set(bitRateTolerance);
	}

	public IntegerProperty videoCoderBitRateToleranceProperty() {
		return videoCoderBitRateTolerance.property();
	}

	private ShadowField<IntegerProperty, Number> videoCoderGlobalQuality = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_VIDEO_CODER_GLOBAL_QUALITY);

	@Override
	public int getVideoCoderGlobalQuality() {
		return videoCoderGlobalQuality.get().intValue();
	}

	@Override
	public void setVideoCoderGlobalQuality(int globalQuality) {
		this.videoCoderGlobalQuality.set(globalQuality);
	}

	public IntegerProperty videoCoderGlobalQualityProperty() {
		return videoCoderGlobalQuality.property();
	}

	private ShadowField<ObjectProperty<VideoCoderPreset>, VideoCoderPreset> videoCoderPreset = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_VIDEO_CODER_PRESET);

	@Enumerated(EnumType.STRING)
	@Override
	public VideoCoderPreset getVideoCoderPreset() {
		return videoCoderPreset.get();
	}

	@Override
	public void setVideoCoderPreset(VideoCoderPreset videoEncoderPreset) {
		this.videoCoderPreset.set(videoEncoderPreset);
	}

	public ObjectProperty<VideoCoderPreset> sidVideoEncoderPresetProperty() {
		return videoCoderPreset.property();
	}

	private ShadowField<IntegerProperty, Number> delay = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY);

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

	private ShadowField<IntegerProperty, Number> delayDryLevel = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY_DRY_LEVEL);

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

	private ShadowField<IntegerProperty, Number> delayWetLevel = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY_WET_LEVEL);

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

	private ShadowField<IntegerProperty, Number> delayFeedbackLevel = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DELAY_FEEDBACK_LEVEL);

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

	private ShadowField<BooleanProperty, Boolean> reverbBypass = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_REVERB_BYPASS);

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

	private ShadowField<FloatProperty, Number> reverbComb1Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB1_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbComb2Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB2_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbComb3Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB3_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbComb4Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_COMB4_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbAllPass1Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_ALL_PASS1_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbAllPass2Delay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_ALL_PASS2_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbSustainDelay = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_SUSTAIN_DELAY);

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

	private ShadowField<FloatProperty, Number> reverbDryWetMix = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_REVERB_DRY_WET_MIX);

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

	private ShadowField<BooleanProperty, Boolean> exsidFakeStereo = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_EXSID_FAKE_STEREO);

	@Override
	public boolean isExsidFakeStereo() {
		return exsidFakeStereo.get();
	}

	@Override
	public void setExsidFakeStereo(boolean exsidFakeStereo) {
		this.exsidFakeStereo.set(exsidFakeStereo);
	}

	public BooleanProperty exsidFakeStereoProperty() {
		return exsidFakeStereo.property();
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
