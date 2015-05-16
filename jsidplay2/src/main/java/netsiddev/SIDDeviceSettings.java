package netsiddev;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class SIDDeviceSettings {
	private final static String FILE_NAME_PROPERTIES = "jsiddevice.properties";
	private final static String PROPERTY_DEVICE_INDEX = "deviceIndex";
	private final static String PROPERTY_DIGI_BOOST = "digiBoost";
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
	 * @return digi boost value from the settings.
	 */
	public synchronized boolean getDigiBoostEnabled() {
		return Boolean.TRUE.equals(Boolean.valueOf(props
				.getProperty(PROPERTY_DIGI_BOOST)));
	}
	
	/**
	 * @return if external connections are allowed.
	 */
	public synchronized boolean getAllowExternalConnections() {
		return Boolean.TRUE.equals(Boolean.valueOf(props
				.getProperty(ALLOW_EXTERNAL_CONNECTIONS)));
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
	 * @param digiBoost
	 *            specifies if digiBoost should be enabled for 8580 model
	 */
	public synchronized void saveDigiBoost(boolean digiBoost) {
		props.setProperty(PROPERTY_DIGI_BOOST, String.valueOf(digiBoost));
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
