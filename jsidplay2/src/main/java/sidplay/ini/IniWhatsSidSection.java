package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CAPTURE_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_ENABLE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_RETRY_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_START_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_PASSWORD;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_URL;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_USERNAME;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.config.IWhatsSidSection;

/**
 * WhatsSid section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
@Parameters(resourceBundle = "sidplay.ini.IniWhatsSidSection")
public class IniWhatsSidSection extends IniSection implements IWhatsSidSection {
	protected IniWhatsSidSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public boolean isEnable() {
		return iniReader.getPropertyBool("WhatsSid", "Enable", DEFAULT_WHATSSID_ENABLE);
	}

	@Override
	@Parameter(names = { "--whatsSidEnable" }, descriptionKey = "WHATSSID_ENABLE", arity = 1, order = 1045)
	public void setEnable(boolean enable) {
		iniReader.setProperty("WhatsSid", "Enable", enable);
	}

	@Override
	public String getUrl() {
		return iniReader.getPropertyString("WhatsSid", "Url", DEFAULT_WHATSSID_URL);
	}

	@Override
	@Parameter(names = { "--whatsSidUrl" }, descriptionKey = "WHATSSID_URL", order = 1046)
	public void setUrl(String url) {
		iniReader.setProperty("WhatsSid", "Url", url);
	}

	@Override
	public String getUsername() {
		return iniReader.getPropertyString("WhatsSid", "Username", DEFAULT_WHATSSID_USERNAME);
	}

	@Override
	@Parameter(names = { "--whatsSidUsername" }, descriptionKey = "WHATSSID_USERNAME", order = 1047)
	public void setUsername(String username) {
		iniReader.setProperty("WhatsSid", "Username", username);
	}

	@Override
	public String getPassword() {
		return iniReader.getPropertyString("WhatsSid", "Password", DEFAULT_WHATSSID_PASSWORD);
	}

	@Override
	@Parameter(names = { "--whatsSidPassword" }, descriptionKey = "WHATSSID_PASSWORD", order = 1048)
	public void setPassword(String password) {
		iniReader.setProperty("WhatsSid", "Password", password);
	}

	@Override
	public int getCaptureTime() {
		return iniReader.getPropertyInt("WhatsSid", "Capture Time", DEFAULT_WHATSSID_CAPTURE_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSidCaptureTime" }, descriptionKey = "WHATSSID_CAPTURE_TIME", order = 1049)
	public void setCaptureTime(int captureTime) {
		iniReader.setProperty("WhatsSid", "Capture Time", captureTime);
	}

	@Override
	public int getMatchStartTime() {
		return iniReader.getPropertyInt("WhatsSid", "Match Start Time", DEFAULT_WHATSSID_MATCH_START_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSidMatchStartTime" }, descriptionKey = "WHATSSID_MATCH_START_TIME", order = 1050)
	public void setMatchStartTime(int matchStartTime) {
		iniReader.setProperty("WhatsSid", "Match Start Time", matchStartTime);
	}

	@Override
	public int getMatchRetryTime() {
		return iniReader.getPropertyInt("WhatsSid", "Match Retry Time", DEFAULT_WHATSSID_MATCH_RETRY_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSidMatchRetryTime" }, descriptionKey = "WHATSSID_MATCH_RETRY_TIME", order = 1051)
	public void setMatchRetryTime(int matchRetryTime) {
		iniReader.setProperty("WhatsSid", "Match Retry Time", matchRetryTime);
	}

	@Override
	public float getMinimumRelativeConfidence() {
		return iniReader.getPropertyFloat("WhatsSid", "Minimum Relative Confidence", DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);
	}

	@Override
	@Parameter(names = { "--whatsSidMinimumRelativeConfidence" }, descriptionKey = "WHATSSID_MINIMUM_RELATIVE_CONFIDENCE", order = 1053)
	public void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
		iniReader.setProperty("WhatsSid", "Minimum Relative Confidence", minimumRelativeConfidence);
	}

}
