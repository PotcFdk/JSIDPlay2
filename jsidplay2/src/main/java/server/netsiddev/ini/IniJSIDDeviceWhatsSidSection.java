package server.netsiddev.ini;

import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

public class IniJSIDDeviceWhatsSidSection extends IniSection {

	public IniJSIDDeviceWhatsSidSection(IniReader iniReader) {
		super(iniReader);
	}

	public boolean isEnable() {
		return iniReader.getPropertyBool("WhatsSID", "Enable", false);
	}

	public String getUrl() {
		return iniReader.getPropertyString("WhatsSID", "Url",
				"https://haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST");
	}

	public String getUsername() {
		return iniReader.getPropertyString("WhatsSID", "Username", "jsidplay2");
	}

	public String getPassword() {
		return iniReader.getPropertyString("WhatsSID", "Password", "jsidplay2!");
	}

	public int getConnectionTimeout() {
		return iniReader.getPropertyInt("WhatsSID", "Connection Timeout", 5000);
	}

	public int getCaptureTime() {
		return iniReader.getPropertyInt("WhatsSID", "Capture Time", 15);
	}

	public int getMatchRetryTime() {
		return iniReader.getPropertyInt("WhatsSID", "Match Retry Time", 10);
	}

	public float getMinimumRelativeConfidence() {
		return iniReader.getPropertyFloat("WhatsSID", "Minimum Relative Confidence", 10f);
	}

}
