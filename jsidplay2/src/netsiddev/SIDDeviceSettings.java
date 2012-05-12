package netsiddev;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SIDDeviceSettings {
	private Properties props;
	
	private final static String FILE_NAME_PROPERTIES = "jsiddevice.properties";
	private final static String PROPERTY_DEVICE_INDEX = "deviceIndex";
	private final static String PROPERTY_DEVICE_INDEX_COMMENT = "JSIDDevice settings";
	
	private static final SIDDeviceSettings instance = new SIDDeviceSettings();
	
	private SIDDeviceSettings() {
		props = new java.util.Properties(); 
		try {
			FileInputStream fis = new FileInputStream(FILE_NAME_PROPERTIES);
			try {
				props.load(fis);
			} finally {
				fis.close();
			}
		} catch (IOException ioe) {
		}
	}
	
	/**
	 * getInstance gets the instance of SIDDeviceSettings
	 * @return instance of SIDDeviceSettings
	 */
	public static SIDDeviceSettings getInstance() {
        return instance;
	}
	
	/**
	 * @return device index. If not found then null is returned.
	 */
	public synchronized int getDeviceIndex() {
		Integer deviceIndex;
		final String deviceIndexString = props.getProperty(PROPERTY_DEVICE_INDEX);
		try {
			deviceIndex = Integer.valueOf(deviceIndexString);
		} catch (NumberFormatException nfe) {
			deviceIndex = 0;
		}
		return deviceIndex;
	}
	
	/**
	 * @param deviceIndex the device index to be saved
	 */
	public synchronized void saveDeviceIndex(final Integer deviceIndex) {
		final Properties props = new java.util.Properties(); 
		props.setProperty(PROPERTY_DEVICE_INDEX, String.valueOf(deviceIndex));
		try {
			props.store(new FileOutputStream(FILE_NAME_PROPERTIES), PROPERTY_DEVICE_INDEX_COMMENT);
		} catch (IOException e1) {
		}
	}
}
