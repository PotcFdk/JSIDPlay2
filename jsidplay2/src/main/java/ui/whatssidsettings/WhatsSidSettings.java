package ui.whatssidsettings;

import static javafx.beans.binding.Bindings.bindBidirectional;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CAPTURE_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_ENABLE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_RETRY_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_START_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_PASSWORD;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_URL;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_USERNAME;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;
import sidplay.Player;
import ui.common.C64Window;
import ui.entities.config.WhatsSidSection;

public class WhatsSidSettings extends C64Window {

	@FXML
	private CheckBox enable;

	@FXML
	private TextField url, username, password, captureTime, matchStartTime, matchRetryTime, minimumRelativeConfidence;

	public WhatsSidSettings() {
		super();
	}

	public WhatsSidSettings(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		WhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

		enable.selectedProperty().bindBidirectional(whatsSidSection.enableProperty());
		url.textProperty().bindBidirectional(whatsSidSection.urlProperty());
		username.textProperty().bindBidirectional(whatsSidSection.usernameProperty());
		password.textProperty().bindBidirectional(whatsSidSection.passwordProperty());
		bindBidirectional(captureTime.textProperty(), whatsSidSection.captureTimeProperty(),
				new IntegerStringConverter());
		bindBidirectional(matchStartTime.textProperty(), whatsSidSection.matchStartTimeProperty(),
				new IntegerStringConverter());
		bindBidirectional(matchRetryTime.textProperty(), whatsSidSection.matchRetryTimeProperty(),
				new IntegerStringConverter());
		bindBidirectional(minimumRelativeConfidence.textProperty(), whatsSidSection.minimumRelativeConfidenceProperty(),
				new NumberStringConverter());

	}

	@FXML
	private void restoreDefaults() {
		WhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

		whatsSidSection.setEnable(DEFAULT_WHATSSID_ENABLE);
		whatsSidSection.setUrl(DEFAULT_WHATSSID_URL);
		whatsSidSection.setUsername(DEFAULT_WHATSSID_USERNAME);
		whatsSidSection.setPassword(DEFAULT_WHATSSID_PASSWORD);
		whatsSidSection.setCaptureTime(DEFAULT_WHATSSID_CAPTURE_TIME);
		whatsSidSection.setMatchStartTime(DEFAULT_WHATSSID_MATCH_START_TIME);
		whatsSidSection.setMatchRetryTime(DEFAULT_WHATSSID_MATCH_RETRY_TIME);
		whatsSidSection.setMinimumRelativeConfidence(DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);
	}

}
