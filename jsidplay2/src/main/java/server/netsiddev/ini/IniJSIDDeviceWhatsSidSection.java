package server.netsiddev.ini;

import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

public class IniJSIDDeviceWhatsSidSection extends IniSection {

	public IniJSIDDeviceWhatsSidSection(IniReader iniReader) {
		super(iniReader);
	}

	public boolean isEnable() {
		return iniReader.getPropertyBool("WhatsSid", "Enable", false);
	}

	public String getUrl() {
		return iniReader.getPropertyString("WhatsSid", "Url",
				"https://haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST");
	}

	public String getUsername() {
		return iniReader.getPropertyString("WhatsSid", "Username", "jsidplay2");
	}

	public String getPassword() {
		return iniReader.getPropertyString("WhatsSid", "Password", "jsidplay2!");
	}

	public int getCaptureTime() {
		return iniReader.getPropertyInt("WhatsSid", "Capture Time", 15);
	}

	public int getMatchRetryTime() {
		return iniReader.getPropertyInt("WhatsSid", "Match Retry Time", 10);
	}

	public float getMinimumRelativeConfidence() {
		return iniReader.getPropertyFloat("WhatsSid", "Minimum Relative Confidence", 4.5f);
	}

}
