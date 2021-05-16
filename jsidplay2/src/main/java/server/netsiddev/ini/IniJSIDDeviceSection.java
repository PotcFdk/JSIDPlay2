package server.netsiddev.ini;

import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

/**
 * SIDPlay2 section of the INI file.
 *
 * @author Ken Händel
 *
 */
public class IniJSIDDeviceSection extends IniSection {

	private static final String SECTION_ID = "JSIDDevice";

	protected IniJSIDDeviceSection(final IniReader ini) {
		super(ini);
	}

	public final int getVersion() {
		return iniReader.getPropertyInt(SECTION_ID, "Version", JSIDDeviceConfig.REQUIRED_CONFIG_VERSION);
	}

	public final void setVersion(final int version) {
		iniReader.setProperty(SECTION_ID, "Version", version);
	}

	public final String getHostname() {
		return iniReader.getPropertyString(SECTION_ID, "Hostname", null);
	}

	public final void setHostname(final String hostname) {
		iniReader.setProperty(SECTION_ID, "Hostname", hostname);
	}

	public final int getPort() {
		return iniReader.getPropertyInt(SECTION_ID, "Port", 0);
	}

	public final void setProxyPort(final int port) {
		iniReader.setProperty(SECTION_ID, "Port", port);
	}

	public int getLatency() {
		return iniReader.getPropertyInt(SECTION_ID, "Latency", 0);
	}
}