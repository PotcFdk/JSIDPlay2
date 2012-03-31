package netsiddev.ini;

import sidplay.ini.IniReader;
import sidplay.ini.IniSection;



/**
 * SIDPlay2 section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniJSIDDeviceSection extends IniSection {
	protected IniJSIDDeviceSection(final IniReader ini) {
		super(ini);
	}
	
	public final int getVersion() {
		return iniReader.getPropertyInt("JSIDDevice", "Version", JSIDDeviceConfig.REQUIRED_CONFIG_VERSION);
	}

	public final String getHostname() {
		return iniReader.getPropertyString("JSIDDevice", "Hostname", null);
	}
	public final void setHostname(final String hostname) {
		iniReader.setProperty("JSIDDevice", "Hostname", hostname);
	}

	public final int getPort() {
		return iniReader.getPropertyInt("JSIDDevice", "Port", 0);
	}
	public final void setProxyPort(final int port) {
		iniReader.setProperty("JSIDDevice", "Port", port);
	}

	public int getLatency() {
		return iniReader.getPropertyInt("JSIDDevice", "Latency", 0);
	}
}