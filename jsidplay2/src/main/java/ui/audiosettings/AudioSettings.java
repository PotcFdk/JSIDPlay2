package ui.audiosettings;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_CODER_BIT_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE;
import static sidplay.ini.IniDefaults.DEFAULT_CBR;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_DRY_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_FEEDBACK_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_WET_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_EXSID_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_MP3_FILE;
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
import static sidplay.ini.IniDefaults.DEFAULT_VBR;
import static sidplay.ini.IniDefaults.DEFAULT_VBR_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_BIT_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_GLOBAL_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_GOP;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_PRESET;

import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;
import libsidplay.common.VideoCoderPreset;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.converter.EnumToStringConverter;
import ui.common.converter.NumberToStringConverter;
import ui.entities.config.AudioSection;

public class AudioSettings extends C64Window {

	@FXML
	private CheckBox bypassDelay, bypassReverb;

	@FXML
	private Slider delay, delayWetLevel, delayDryLevel, delayFeedbackLevel;

	@FXML
	private Slider reverbComb1Delay, reverbComb2Delay, reverbComb3Delay, reverbComb4Delay;

	@FXML
	private Slider reverbAllPass1Delay, reverbAllPass2Delay;

	@FXML
	private Slider reverbSustainDelay, reverbDryWetMix;

	@FXML
	private Label delayValue, delayWetLevelValue, delayDryLevelValue, delayFeedbackLevelValue;

	@FXML
	private Label reverbComb1DelayValue, reverbComb2DelayValue, reverbComb3DelayValue, reverbComb4DelayValue;

	@FXML
	private Label reverbAllPass1DelayValue, reverbAllPass2DelayValue;

	@FXML
	private Label reverbSustainDelayValue, reverbDryWetMixValue;

	@FXML
	private Label cbrLabel, vbrQualityLabel, audioCoderBitRateLabel, audioCoderBitRateToleranceLabel,
			videoCoderGOPLabel, videoCoderBitRateLabel, videoCoderBitRateToleranceLabel, videoCoderGlobalQualityLabel,
			videoCoderPresetNameLabel;

	@FXML
	private TextField cbr, vbrQuality, audioCoderBitRate, audioCoderBitRateTolerance, videoCoderGOP, videoCoderBitRate,
			videoCoderBitRateTolerance, videoCoderGlobalQuality;

	@FXML
	private ComboBox<VideoCoderPreset> videoCoderPreset;

	@FXML
	private CheckBox vbr, exsidFakeStereo;

	private ObservableList<VideoCoderPreset> videoCoderPresets;

	public AudioSettings() {
		super();
	}

	public AudioSettings(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		ResourceBundle bundle = util.getBundle();
		AudioSection audioSection = util.getConfig().getAudioSection();

		bypassDelay.selectedProperty().bindBidirectional(audioSection.delayBypassProperty());

		delay.setLabelFormatter(new NumberToStringConverter<Double>(0));
		delay.valueProperty().bindBidirectional(audioSection.delayProperty());
		delayValue.textProperty().bindBidirectional(audioSection.delayProperty(), new NumberToStringConverter<>(0));

		delayWetLevel.setLabelFormatter(new NumberToStringConverter<Double>(0));
		delayWetLevel.valueProperty().bindBidirectional(audioSection.delayWetLevelProperty());
		delayWetLevelValue.textProperty().bindBidirectional(audioSection.delayWetLevelProperty(),
				new NumberToStringConverter<>(0));

		delayDryLevel.setLabelFormatter(new NumberToStringConverter<Double>(0));
		delayDryLevel.valueProperty().bindBidirectional(audioSection.delayDryLevelProperty());
		delayDryLevelValue.textProperty().bindBidirectional(audioSection.delayDryLevelProperty(),
				new NumberToStringConverter<>(0));

		delayFeedbackLevel.setLabelFormatter(new NumberToStringConverter<Double>(0));
		delayFeedbackLevel.valueProperty().bindBidirectional(audioSection.delayFeedbackLevelProperty());
		delayFeedbackLevelValue.textProperty().bindBidirectional(audioSection.delayFeedbackLevelProperty(),
				new NumberToStringConverter<>(0));

		bypassReverb.selectedProperty().bindBidirectional(audioSection.reverbBypassProperty());

		reverbComb1Delay.setLabelFormatter(new NumberToStringConverter<Double>(1));
		reverbComb1Delay.valueProperty().bindBidirectional(audioSection.reverbComb1DelayProperty());
		reverbComb1DelayValue.textProperty().bindBidirectional(audioSection.reverbComb1DelayProperty(),
				new NumberToStringConverter<>(1));

		reverbComb2Delay.setLabelFormatter(new NumberToStringConverter<Double>(1));
		reverbComb2Delay.valueProperty().bindBidirectional(audioSection.reverbComb2DelayProperty());
		reverbComb2DelayValue.textProperty().bindBidirectional(audioSection.reverbComb2DelayProperty(),
				new NumberToStringConverter<>(1));

		reverbComb3Delay.setLabelFormatter(new NumberToStringConverter<Double>(1));
		reverbComb3Delay.valueProperty().bindBidirectional(audioSection.reverbComb3DelayProperty());
		reverbComb3DelayValue.textProperty().bindBidirectional(audioSection.reverbComb3DelayProperty(),
				new NumberToStringConverter<>(1));

		reverbComb4Delay.setLabelFormatter(new NumberToStringConverter<Double>(1));
		reverbComb4Delay.valueProperty().bindBidirectional(audioSection.reverbComb4DelayProperty());
		reverbComb4DelayValue.textProperty().bindBidirectional(audioSection.reverbComb4DelayProperty(),
				new NumberToStringConverter<>(1));

		reverbAllPass1Delay.setLabelFormatter(new NumberToStringConverter<Double>(1));
		reverbAllPass1Delay.valueProperty().bindBidirectional(audioSection.reverbAllPass1DelayProperty());
		reverbAllPass1DelayValue.textProperty().bindBidirectional(audioSection.reverbAllPass1DelayProperty(),
				new NumberToStringConverter<>(1));

		reverbAllPass2Delay.setLabelFormatter(new NumberToStringConverter<Double>(1));
		reverbAllPass2Delay.valueProperty().bindBidirectional(audioSection.reverbAllPass2DelayProperty());
		reverbAllPass2DelayValue.textProperty().bindBidirectional(audioSection.reverbAllPass2DelayProperty(),
				new NumberToStringConverter<>(1));

		reverbSustainDelay.setLabelFormatter(new NumberToStringConverter<Double>(0));
		reverbSustainDelay.valueProperty().bindBidirectional(audioSection.reverbSustainDelayProperty());
		reverbSustainDelayValue.textProperty().bindBidirectional(audioSection.reverbSustainDelayProperty(),
				new NumberToStringConverter<>(0));

		reverbDryWetMix.setLabelFormatter(new NumberToStringConverter<Double>(2));
		reverbDryWetMix.valueProperty().bindBidirectional(audioSection.reverbDryWetMixProperty());
		reverbDryWetMixValue.textProperty().bindBidirectional(audioSection.reverbDryWetMixProperty(),
				new NumberToStringConverter<>(2));

		cbr.textProperty().bindBidirectional(audioSection.cbrProperty(), new IntegerStringConverter());
		vbr.selectedProperty().bindBidirectional(audioSection.vbrProperty());
		vbrQuality.textProperty().bindBidirectional(audioSection.vbrQualityProperty(), new IntegerStringConverter());

		audioCoderBitRate.textProperty().bindBidirectional(audioSection.audioCoderBitRateProperty(),
				new NumberStringConverter());
		audioCoderBitRateTolerance.textProperty().bindBidirectional(audioSection.audioCoderBitRateToleranceProperty(),
				new NumberStringConverter());
		videoCoderGOP.textProperty().bindBidirectional(audioSection.videoCoderNumPicturesInGroupOfPicturesProperty(),
				new NumberStringConverter());
		videoCoderBitRate.textProperty().bindBidirectional(audioSection.videoCoderBitRateProperty(),
				new NumberStringConverter());
		videoCoderBitRateTolerance.textProperty().bindBidirectional(audioSection.videoCoderBitRateToleranceProperty(),
				new NumberStringConverter());
		videoCoderGlobalQuality.textProperty().bindBidirectional(audioSection.videoCoderGlobalQualityProperty(),
				new NumberStringConverter());

		videoCoderPresets = FXCollections.<VideoCoderPreset>observableArrayList(VideoCoderPreset.values());
		videoCoderPreset.setConverter(new EnumToStringConverter<VideoCoderPreset>(bundle));
		videoCoderPreset.valueProperty().bindBidirectional(audioSection.sidVideoEncoderPresetProperty());
		videoCoderPreset.setItems(videoCoderPresets);

		exsidFakeStereo.selectedProperty().bindBidirectional(audioSection.exsidFakeStereoProperty());
	}

	@FXML
	private void restoreDefaults() {
		AudioSection audioSection = util.getConfig().getAudioSection();

		audioSection.setDelayBypass(DEFAULT_DELAY_BYPASS);
		audioSection.setDelay(DEFAULT_DELAY);
		audioSection.setDelayWetLevel(DEFAULT_DELAY_WET_LEVEL);
		audioSection.setDelayDryLevel(DEFAULT_DELAY_DRY_LEVEL);
		audioSection.setDelayFeedbackLevel(DEFAULT_DELAY_FEEDBACK_LEVEL);

		audioSection.setReverbBypass(DEFAULT_REVERB_BYPASS);
		audioSection.setReverbComb1Delay(DEFAULT_REVERB_COMB1_DELAY);
		audioSection.setReverbComb2Delay(DEFAULT_REVERB_COMB2_DELAY);
		audioSection.setReverbComb3Delay(DEFAULT_REVERB_COMB3_DELAY);
		audioSection.setReverbComb4Delay(DEFAULT_REVERB_COMB4_DELAY);
		audioSection.setReverbAllPass1Delay(DEFAULT_REVERB_ALL_PASS1_DELAY);
		audioSection.setReverbAllPass2Delay(DEFAULT_REVERB_ALL_PASS2_DELAY);
		audioSection.setReverbSustainDelay(DEFAULT_REVERB_SUSTAIN_DELAY);
		audioSection.setReverbDryWetMix(DEFAULT_REVERB_DRY_WET_MIX);

		audioSection.setCbr(DEFAULT_CBR);
		audioSection.setVbr(DEFAULT_VBR);
		audioSection.setVbrQuality(DEFAULT_VBR_QUALITY);
		audioSection.setMp3(DEFAULT_MP3_FILE);
		audioSection.setPlayOriginal(DEFAULT_PLAY_ORIGINAL);

		audioSection.setAudioCoderBitRate(DEFAULT_AUDIO_CODER_BIT_RATE);
		audioSection.setAudioCoderBitRateTolerance(DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE);
		audioSection.setVideoCoderNumPicturesInGroupOfPictures(DEFAULT_VIDEO_CODER_GOP);
		audioSection.setVideoCoderBitRate(DEFAULT_VIDEO_CODER_BIT_RATE);
		audioSection.setVideoCoderBitRateTolerance(DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE);
		audioSection.setVideoCoderGlobalQuality(DEFAULT_VIDEO_CODER_GLOBAL_QUALITY);
		audioSection.setVideoCoderPreset(DEFAULT_VIDEO_CODER_PRESET);

		audioSection.setExsidFakeStereo(DEFAULT_EXSID_FAKE_STEREO);
	}

}
