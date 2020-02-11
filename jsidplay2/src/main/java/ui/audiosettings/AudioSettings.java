package ui.audiosettings;

import static sidplay.ini.IniDefaults.DEFAULT_AVI_COMPRESSION_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_CBR;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_DRY_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_FEEDBACK_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_WET_LEVEL;
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

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.NumberToStringConverter;
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
	private Label cbrLabel, vbrQualityLabel, aviVideoQualityLabel;
	
	@FXML
	private TextField cbr, vbrQuality, aviVideoQuality;
	
	@FXML
	private CheckBox vbr;
	
	public AudioSettings() {
		super();
	}

	public AudioSettings(Player player) {
		super(player);
	}

	@FXML
	protected void initialize() {
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

		aviVideoQuality.textProperty().bindBidirectional(audioSection.aviCompressionQualityProperty(),
				new NumberStringConverter());
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
		
		audioSection.setAviCompressionQuality(DEFAULT_AVI_COMPRESSION_QUALITY);
	}

}
