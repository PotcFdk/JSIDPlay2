package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CAPTURE_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_ENABLE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_RETRY_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_START_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_PASSWORD;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_URL;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_USERNAME;

import javax.persistence.Embeddable;

import com.beust.jcommander.Parameters;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.config.IWhatsSidSection;

@Embeddable
@Parameters(resourceBundle = "ui.entities.config.WhatsSidSection")
public class WhatsSidSection implements IWhatsSidSection {

	private BooleanProperty enableProperty = new SimpleBooleanProperty(DEFAULT_WHATSSID_ENABLE);

	@Override
	public boolean isEnable() {
		return enableProperty.get();
	}

	@Override
	public void setEnable(boolean enable) {
		this.enableProperty.set(enable);
	}

	public BooleanProperty enableProperty() {
		return enableProperty;
	}

	private StringProperty urlProperty = new SimpleStringProperty(DEFAULT_WHATSSID_URL);

	@Override
	public String getUrl() {
		return urlProperty.get();
	}

	@Override
	public void setUrl(String url) {
		this.urlProperty.set(url);
	}

	public StringProperty urlProperty() {
		return urlProperty;
	}

	private StringProperty usernameProperty = new SimpleStringProperty(DEFAULT_WHATSSID_USERNAME);

	@Override
	public String getUsername() {
		return usernameProperty.get();
	}

	@Override
	public void setUsername(String username) {
		this.usernameProperty.set(username);
	}

	public StringProperty usernameProperty() {
		return usernameProperty;
	}

	private StringProperty passwordProperty = new SimpleStringProperty(DEFAULT_WHATSSID_PASSWORD);

	@Override
	public String getPassword() {
		return passwordProperty.get();
	}

	@Override
	public void setPassword(String password) {
		this.passwordProperty.set(password);
	}

	public StringProperty passwordProperty() {
		return passwordProperty;
	}

	private ObjectProperty<Integer> captureTimeProperty = new SimpleObjectProperty<>(
			DEFAULT_WHATSSID_CAPTURE_TIME);

	@Override
	public int getCaptureTime() {
		return captureTimeProperty.get();
	}

	@Override
	public void setCaptureTime(int captureTime) {
		this.captureTimeProperty.set(captureTime);
	}

	public ObjectProperty<Integer> captureTimeProperty() {
		return captureTimeProperty;
	}

	private ObjectProperty<Integer> matchStartTimeProperty = new SimpleObjectProperty<>(
			DEFAULT_WHATSSID_MATCH_START_TIME);

	@Override
	public int getMatchStartTime() {
		return matchStartTimeProperty.get();
	}

	@Override
	public void setMatchStartTime(int matchStartTime) {
		this.matchStartTimeProperty.set(matchStartTime);
	}

	public ObjectProperty<Integer> matchStartTimeProperty() {
		return matchStartTimeProperty;
	}

	private ObjectProperty<Integer> matchRetryTimeProperty = new SimpleObjectProperty<>(
			DEFAULT_WHATSSID_MATCH_RETRY_TIME);

	@Override
	public int getMatchRetryTime() {
		return matchRetryTimeProperty.get();
	}

	@Override
	public void setMatchRetryTime(int matchRetryTime) {
		matchRetryTimeProperty.set(matchRetryTime);
	}

	public ObjectProperty<Integer> matchRetryTimeProperty() {
		return matchRetryTimeProperty;
	}

	private FloatProperty minimumRelativeConfidenceProperty = new SimpleFloatProperty(
			DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);

	@Override
	public float getMinimumRelativeConfidence() {
		return minimumRelativeConfidenceProperty.get();
	}

	@Override
	public void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
		minimumRelativeConfidenceProperty.set(minimumRelativeConfidence);
	}

	public FloatProperty minimumRelativeConfidenceProperty() {
		return minimumRelativeConfidenceProperty;
	}
}
