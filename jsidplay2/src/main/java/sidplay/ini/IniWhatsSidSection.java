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
 * WhatsSID section of the INI file.
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
		return iniReader.getPropertyBool("WhatsSID", "Enable", DEFAULT_WHATSSID_ENABLE);
	}

	@Override
	@Parameter(names = { "--whatsSIDEnable" }, descriptionKey = "WHATSSID_ENABLE", arity = 1, order = 1045)
	public void setEnable(boolean enable) {
		iniReader.setProperty("WhatsSID", "Enable", enable);
	}

	@Override
	public String getUrl() {
		return iniReader.getPropertyString("WhatsSID", "Url", DEFAULT_WHATSSID_URL);
	}

	@Override
	@Parameter(names = { "--whatsSIDUrl" }, descriptionKey = "WHATSSID_URL", order = 1046)
	public void setUrl(String url) {
		iniReader.setProperty("WhatsSID", "Url", url);
	}

	@Override
	public String getUsername() {
		return iniReader.getPropertyString("WhatsSID", "Username", DEFAULT_WHATSSID_USERNAME);
	}

	@Override
	@Parameter(names = { "--whatsSIDUsername" }, descriptionKey = "WHATSSID_USERNAME", order = 1047)
	public void setUsername(String username) {
		iniReader.setProperty("WhatsSID", "Username", username);
	}

	@Override
	public String getPassword() {
		return iniReader.getPropertyString("WhatsSID", "Password", DEFAULT_WHATSSID_PASSWORD);
	}

	@Override
	@Parameter(names = { "--whatsSIDPassword" }, descriptionKey = "WHATSSID_PASSWORD", order = 1048)
	public void setPassword(String password) {
		iniReader.setProperty("WhatsSID", "Password", password);
	}

	@Override
	public int getCaptureTime() {
		return iniReader.getPropertyInt("WhatsSID", "Capture Time", DEFAULT_WHATSSID_CAPTURE_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSIDCaptureTime" }, descriptionKey = "WHATSSID_CAPTURE_TIME", order = 1049)
	public void setCaptureTime(int captureTime) {
		iniReader.setProperty("WhatsSID", "Capture Time", captureTime);
	}

	@Override
	public int getMatchStartTime() {
		return iniReader.getPropertyInt("WhatsSID", "Match Start Time", DEFAULT_WHATSSID_MATCH_START_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSIDMatchStartTime" }, descriptionKey = "WHATSSID_MATCH_START_TIME", order = 1050)
	public void setMatchStartTime(int matchStartTime) {
		iniReader.setProperty("WhatsSID", "Match Start Time", matchStartTime);
	}

	@Override
	public int getMatchRetryTime() {
		return iniReader.getPropertyInt("WhatsSID", "Match Retry Time", DEFAULT_WHATSSID_MATCH_RETRY_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSIDMatchRetryTime" }, descriptionKey = "WHATSSID_MATCH_RETRY_TIME", order = 1051)
	public void setMatchRetryTime(int matchRetryTime) {
		iniReader.setProperty("WhatsSID", "Match Retry Time", matchRetryTime);
	}

	@Override
	public float getMinimumRelativeConfidence() {
		return iniReader.getPropertyFloat("WhatsSID", "Minimum Relative Confidence",
				DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);
	}

	@Override
	@Parameter(names = {
			"--whatsSIDMinimumRelativeConfidence" }, descriptionKey = "WHATSSID_MINIMUM_RELATIVE_CONFIDENCE", order = 1053)
	public void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
		iniReader.setProperty("WhatsSID", "Minimum Relative Confidence", minimumRelativeConfidence);
	}

}
