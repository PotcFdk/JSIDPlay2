package netsiddev;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SIDDeviceSettings {
	private Properties props;

	private final static String FILE_NAME_PROPERTIES = "jsiddevice.properties";
	private final static String PROPERTY_DEVICE_INDEX = "deviceIndex";
	private final static String PROPERTY_DIGI_BOOST = "digiBoost";
	private final static String PROPERTY_DEVICE_INDEX_COMMENT = "JSIDDevice settings";

	private static final SIDDeviceSettings instance = new SIDDeviceSettings();

	private SIDDeviceSettings() {
		props = new java.util.Properties();
		try (FileInputStream fis = new FileInputStream(FILE_NAME_PROPERTIES)) {
			props.load(fis);
		} catch (IOException ioe) {
		}
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
		return Boolean.TRUE.equals(Boolean.valueOf(
				props.getProperty(PROPERTY_DIGI_BOOST)).booleanValue());
	}

	/**
	 * @param deviceIndex
	 *            the device index to be saved
	 */
	public synchronized void saveDeviceIndex(final Integer deviceIndex) {
		props.setProperty(PROPERTY_DEVICE_INDEX, String.valueOf(deviceIndex));
		try {
			props.store(new FileOutputStream(FILE_NAME_PROPERTIES),
					PROPERTY_DEVICE_INDEX_COMMENT);
		} catch (IOException e1) {
		}
	}

	/**
	 * @param digiBoost
	 *            specifies if digiBoost should be enabled for 8580 model
	 */
	public synchronized void saveDigiBoost(boolean digiBoost) {
		props.setProperty(PROPERTY_DIGI_BOOST, String.valueOf(digiBoost));
		try {
			props.store(new FileOutputStream(FILE_NAME_PROPERTIES),
					PROPERTY_DEVICE_INDEX_COMMENT);
		} catch (IOException e1) {
		}
	}
}
