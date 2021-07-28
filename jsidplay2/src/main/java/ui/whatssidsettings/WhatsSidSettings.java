package ui.whatssidsettings;

import static javafx.beans.binding.Bindings.bindBidirectional;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CAPTURE_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CONNECTION_TIMEOUT;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_DETECT_CHIP_MODEL;
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
import libsidplay.config.IWhatsSidSection;
import sidplay.Player;
import sidplay.fingerprinting.FingerprintJsonClient;
import ui.common.C64Window;
import ui.entities.config.WhatsSidSection;

public class WhatsSidSettings extends C64Window {

	@FXML
	private CheckBox enable;

	@FXML
	private TextField url, username, password, connectionTimeout, captureTime, matchStartTime, matchRetryTime,
			minimumRelativeConfidence;

	@FXML
	private CheckBox detectWhatsSidChipModel;

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
		url.textProperty().addListener((s, o, n) -> updateWhatsSid());
		username.textProperty().bindBidirectional(whatsSidSection.usernameProperty());
		username.textProperty().addListener((s, o, n) -> updateWhatsSid());
		password.textProperty().bindBidirectional(whatsSidSection.passwordProperty());
		password.textProperty().addListener((s, o, n) -> updateWhatsSid());
		bindBidirectional(connectionTimeout.textProperty(), whatsSidSection.connectionTimeoutProperty(),
				new IntegerStringConverter());
		connectionTimeout.textProperty().addListener((s, o, n) -> updateWhatsSid());
		bindBidirectional(captureTime.textProperty(), whatsSidSection.captureTimeProperty(),
				new IntegerStringConverter());
		bindBidirectional(matchStartTime.textProperty(), whatsSidSection.matchStartTimeProperty(),
				new IntegerStringConverter());
		bindBidirectional(matchRetryTime.textProperty(), whatsSidSection.matchRetryTimeProperty(),
				new IntegerStringConverter());
		bindBidirectional(minimumRelativeConfidence.textProperty(), whatsSidSection.minimumRelativeConfidenceProperty(),
				new NumberStringConverter());
		detectWhatsSidChipModel.selectedProperty().bindBidirectional(whatsSidSection.detectChipModelProperty());
	}

	@FXML
	private void restoreDefaults() {
		WhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

		whatsSidSection.setEnable(DEFAULT_WHATSSID_ENABLE);
		whatsSidSection.setUrl(DEFAULT_WHATSSID_URL);
		whatsSidSection.setUsername(DEFAULT_WHATSSID_USERNAME);
		whatsSidSection.setPassword(DEFAULT_WHATSSID_PASSWORD);
		whatsSidSection.setConnectionTimeout(DEFAULT_WHATSSID_CONNECTION_TIMEOUT);
		whatsSidSection.setCaptureTime(DEFAULT_WHATSSID_CAPTURE_TIME);
		whatsSidSection.setMatchStartTime(DEFAULT_WHATSSID_MATCH_START_TIME);
		whatsSidSection.setMatchRetryTime(DEFAULT_WHATSSID_MATCH_RETRY_TIME);
		whatsSidSection.setMinimumRelativeConfidence(DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);
		whatsSidSection.setDetectChipModel(DEFAULT_WHATSSID_DETECT_CHIP_MODEL);
	}

	private void updateWhatsSid() {
		IWhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

		String url = whatsSidSection.getUrl();
		String username = whatsSidSection.getUsername();
		String password = whatsSidSection.getPassword();
		int connectionTimeout = whatsSidSection.getConnectionTimeout();
		util.getPlayer().setFingerPrintMatcher(new FingerprintJsonClient(url, username, password, connectionTimeout));
	}

}
