package server.netsiddev;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import sidplay.audio.AudioConfig;

public class SIDDeviceSettings {
	private final static String FILE_NAME_PROPERTIES = "jsiddevice.properties";
	private final static String PROPERTY_DEVICE_INDEX = "deviceIndex";
	private final static String PROPERTY_DIGI_BOOST = "digiBoost";
	private final static String PROPERTY_WHATSSID_URL = "whatsSIDUrl";
	private final static String PROPERTY_WHATSSID_ENABLE = "whatsSIDEnable";
	private final static String PROPERTY_WHATSSID_USERNAME = "whatsSIDUsername";
	private final static String PROPERTY_WHATSSID_PASSWORD = "whatsSIDPassword";
	private final static String PROPERTY_WHATSSID_CAPTURE_TIME = "whatsSIDCaptureTime";
	private final static String PROPERTY_WHATSSID_MATCH_RETRY_TIME = "whatsSIDMatchRetryTime";
	private final static String PROPERTY_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE = "whatsSIDMinimumRelativeConfidence";
	private final static String PROPERTY_AUDIO_BUFFER_SIZE = "audioBufferSize";
	private final static String ALLOW_EXTERNAL_CONNECTIONS = "allowExternalConnections";
	private final static String PROPERTY_DEVICE_INDEX_COMMENT = "JSIDDevice settings";

	private static final SIDDeviceSettings instance = new SIDDeviceSettings();

	private Properties props = new java.util.Properties();

	private SIDDeviceSettings() {
		load();
	}

	/**
	 * getInstance gets the instance of SIDDeviceSettings
	 * 
	 * @return instance of SIDDeviceSettings
	 */
	public static SIDDeviceSettings getInstance() {
		return instance;
	}

	/**
	 * @return device index. If not found then null is returned.
	 */
	public synchronized int getDeviceIndex() {
		try {
			return Integer.valueOf(props.getProperty(PROPERTY_DEVICE_INDEX));
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	/**
	 * @return device index. If not found then null is returned.
	 */
	public synchronized int getAudioBufferSize() {
		try {
			return Integer.valueOf(props.getProperty(PROPERTY_AUDIO_BUFFER_SIZE));
		} catch (NumberFormatException nfe) {
			return AudioConfig.getDefaultBufferSize();
		}
	}

	/**
	 * @return digi boost value from the settings.
	 */
	public synchronized boolean getDigiBoostEnabled() {
		return Boolean.TRUE.equals(Boolean.valueOf(props.getProperty(PROPERTY_DIGI_BOOST)));
	}

	/**
	 * @return WhatsSID enable from the settings.
	 */
	public synchronized boolean isWhatsSidEnable() {
		return Boolean.TRUE.equals(Boolean.valueOf(props.getProperty(PROPERTY_WHATSSID_ENABLE, "true")));
	}

	/**
	 * @return WhatsSID URL from the settings.
	 */
	public synchronized String getWhatsSidUrl() {
		return props.getProperty(PROPERTY_WHATSSID_URL, "https://haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST");
	}

	/**
	 * @return WhatsSID username from the settings.
	 */
	public synchronized String getWhatsSidUsername() {
		return props.getProperty(PROPERTY_WHATSSID_USERNAME, "jsidplay2");
	}

	/**
	 * @return WhatsSID password from the settings.
	 */
	public synchronized String getWhatsSidPassword() {
		return props.getProperty(PROPERTY_WHATSSID_PASSWORD, "jsidplay2!");
	}

	/**
	 * @return WhatsSID capture time from the settings.
	 */
	public synchronized int getWhatsSidCaptureTime() {
		return Integer.valueOf(props.getProperty(PROPERTY_WHATSSID_CAPTURE_TIME, "15"));
	}

	/**
	 * @return WhatsSID match retry time from the settings.
	 */
	public synchronized int getWhatsSidMatchRetryTime() {
		return Integer.valueOf(props.getProperty(PROPERTY_WHATSSID_MATCH_RETRY_TIME, "15"));
	}

	/**
	 * @return WhatsSID match retry time from the settings.
	 */
	public synchronized double getWhatsSidMinimumRelativeConfidence() {
		return Double.valueOf(props.getProperty(PROPERTY_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE, "4.5"));
	}

	/**
	 * @return if external connections are allowed.
	 */
	public synchronized boolean getAllowExternalConnections() {
		return Boolean.TRUE.equals(Boolean.valueOf(props.getProperty(ALLOW_EXTERNAL_CONNECTIONS)));
	}

	/**
	 * @param deviceIndex
	 *            the device index to be saved
	 */
	public synchronized void saveDeviceIndex(final Integer deviceIndex) {
		props.setProperty(PROPERTY_DEVICE_INDEX, String.valueOf(deviceIndex));
		save();
	}

	/**
	 * @param audioBufferSize
	 *            the audio buffer size to be saved
	 */
	public synchronized void saveAudioBufferSize(final Integer audioBufferSize) {
		props.setProperty(PROPERTY_AUDIO_BUFFER_SIZE, String.valueOf(audioBufferSize));
		save();
	}

	/**
	 * @param digiBoost
	 *            specifies if digiBoost should be enabled for 8580 model
	 */
	public synchronized void saveDigiBoost(boolean digiBoost) {
		props.setProperty(PROPERTY_DIGI_BOOST, String.valueOf(digiBoost));
		save();
	}

	public void saveWhatsSidEnable(boolean whatsSidEnable) {
		props.setProperty(PROPERTY_WHATSSID_ENABLE, String.valueOf(whatsSidEnable));
		save();
	}

	public void saveWhatsSidUrl(String whatsSidUrl) {
		props.setProperty(PROPERTY_WHATSSID_URL, String.valueOf(whatsSidUrl));
		save();
	}

	public void saveWhatsSidUsername(String whatsSidUsername) {
		props.setProperty(PROPERTY_WHATSSID_USERNAME, String.valueOf(whatsSidUsername));
		save();
	}

	public void saveWhatsSidPassword(String whatsSidPassword) {
		props.setProperty(PROPERTY_WHATSSID_PASSWORD, String.valueOf(whatsSidPassword));
		save();
	}

	public void saveWhatsSidCaptureTime(int whatsSidCaptureTime) {
		props.setProperty(PROPERTY_WHATSSID_CAPTURE_TIME, String.valueOf(whatsSidCaptureTime));
		save();
	}

	public void saveWhatsSidMatchRetryTime(int whatsSidMatchRetryTime) {
		props.setProperty(PROPERTY_WHATSSID_MATCH_RETRY_TIME, String.valueOf(whatsSidMatchRetryTime));
		save();
	}

	public void saveWhatsSidMinimumRelativeConfidence(double whatsSidMinimumRelativeConfidence) {
		props.setProperty(PROPERTY_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE, String.valueOf(whatsSidMinimumRelativeConfidence));
		save();
	}

	/**
	 * @param allowExternalConnections
	 *            specifies if external connection are allowed
	 */
	public synchronized void saveAllowExternalConnections(boolean allowExternalConnections) {
		props.setProperty(ALLOW_EXTERNAL_CONNECTIONS, String.valueOf(allowExternalConnections));
		save();
	}

	public void load() {
		try (FileInputStream fis = new FileInputStream(FILE_NAME_PROPERTIES)) {
			props.load(fis);
		} catch (IOException ioe) {
		}
	}

	public void save() {
		try (OutputStream os = new FileOutputStream(FILE_NAME_PROPERTIES)) {
			props.store(os, PROPERTY_DEVICE_INDEX_COMMENT);
		} catch (IOException e1) {
		}
	}
}
