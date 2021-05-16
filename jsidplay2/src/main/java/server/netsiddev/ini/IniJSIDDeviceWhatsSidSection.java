package server.netsiddev.ini;

import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

public class IniJSIDDeviceWhatsSidSection extends IniSection {

	private static final String SECTION_ID = "WhatsSID";

	public IniJSIDDeviceWhatsSidSection(IniReader iniReader) {
		super(iniReader);
	}

	public boolean isEnable() {
		return iniReader.getPropertyBool(SECTION_ID, "Enable", false);
	}

	public String getUrl() {
		return iniReader.getPropertyString(SECTION_ID, "Url",
				"https://haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST");
	}

	public String getUsername() {
		return iniReader.getPropertyString(SECTION_ID, "Username", "jsidplay2");
	}

	public String getPassword() {
		return iniReader.getPropertyString(SECTION_ID, "Password", "jsidplay2!");
	}

	public int getConnectionTimeout() {
		return iniReader.getPropertyInt(SECTION_ID, "Connection Timeout", 5000);
	}

	public int getCaptureTime() {
		return iniReader.getPropertyInt(SECTION_ID, "Capture Time", 15);
	}

	public int getMatchRetryTime() {
		return iniReader.getPropertyInt(SECTION_ID, "Match Retry Time", 10);
	}

	public float getMinimumRelativeConfidence() {
		return iniReader.getPropertyFloat(SECTION_ID, "Minimum Relative Confidence", 10f);
	}

}
