package ui.entities.config;

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
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Embeddable
@Parameters(resourceBundle = "ui.entities.config.WhatsSidSection")
public class WhatsSidSection implements IWhatsSidSection {

	private ShadowField<BooleanProperty, Boolean> enable = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_WHATSSID_ENABLE);

	@Override
	public final boolean isEnable() {
		return enable.get();
	}

	@Override
	public final void setEnable(boolean enable) {
		this.enable.set(enable);
	}

	public final BooleanProperty enableProperty() {
		return enable.property();
	}

	private ShadowField<StringProperty, String> url = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_WHATSSID_URL);

	@Override
	public final String getUrl() {
		return url.get();
	}

	@Override
	public final void setUrl(String url) {
		this.url.set(url);
	}

	public final StringProperty urlProperty() {
		return url.property();
	}

	private ShadowField<StringProperty, String> username = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_WHATSSID_USERNAME);

	@Override
	public final String getUsername() {
		return username.get();
	}

	@Override
	public final void setUsername(String username) {
		this.username.set(username);
	}

	public final StringProperty usernameProperty() {
		return username.property();
	}

	private ShadowField<StringProperty, String> password = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_WHATSSID_PASSWORD);

	@Override
	public final String getPassword() {
		return password.get();
	}

	@Override
	public final void setPassword(String password) {
		this.password.set(password);
	}

	public final StringProperty passwordProperty() {
		return password.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> connectionTimeout = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_WHATSSID_CONNECTION_TIMEOUT);

	@Override
	public final int getConnectionTimeout() {
		return connectionTimeout.get();
	}

	@Override
	public final void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout.set(connectionTimeout);
	}

	public final ObjectProperty<Integer> connectionTimeoutProperty() {
		return connectionTimeout.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> captureTime = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_WHATSSID_CAPTURE_TIME);

	@Override
	public final int getCaptureTime() {
		return captureTime.get();
	}

	@Override
	public final void setCaptureTime(int captureTime) {
		this.captureTime.set(captureTime);
	}

	public final ObjectProperty<Integer> captureTimeProperty() {
		return captureTime.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> matchStartTime = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_WHATSSID_MATCH_START_TIME);

	@Override
	public final int getMatchStartTime() {
		return matchStartTime.get();
	}

	@Override
	public final void setMatchStartTime(int matchStartTime) {
		this.matchStartTime.set(matchStartTime);
	}

	public final ObjectProperty<Integer> matchStartTimeProperty() {
		return matchStartTime.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> matchRetryTime = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_WHATSSID_MATCH_RETRY_TIME);

	@Override
	public final int getMatchRetryTime() {
		return matchRetryTime.get();
	}

	@Override
	public final void setMatchRetryTime(int matchRetryTime) {
		this.matchRetryTime.set(matchRetryTime);
	}

	public final ObjectProperty<Integer> matchRetryTimeProperty() {
		return matchRetryTime.property();
	}

	private ShadowField<FloatProperty, Number> minimumRelativeConfidence = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);

	@Override
	public final float getMinimumRelativeConfidence() {
		return minimumRelativeConfidence.get().floatValue();
	}

	@Override
	public final void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
		this.minimumRelativeConfidence.set(minimumRelativeConfidence);
	}

	public final FloatProperty minimumRelativeConfidenceProperty() {
		return minimumRelativeConfidence.property();
	}

	private ShadowField<BooleanProperty, Boolean> detectChipModel = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_WHATSSID_DETECT_CHIP_MODEL);

	@Override
	public final boolean isDetectChipModel() {
		return detectChipModel.get();
	}

	@Override
	public final void setDetectChipModel(boolean detectChipModel) {
		this.detectChipModel.set(detectChipModel);
	}

	public final BooleanProperty detectChipModelProperty() {
		return detectChipModel.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
