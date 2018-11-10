package libsidplay.common;

public interface HardwareSIDBuilder extends SIDBuilder {

	/**
	 * Get maximum number of supported SID devices.
	 * 
	 * @return maximum number of supported SID devices
	 */
	int getDeviceCount();

	/**
	 * Get device ID of specified SID
	 * 
	 * @param deviceNum SID device number
	 * 
	 * @return device ID of specified SID (null means unassigned)
	 */
	Integer getDeviceId(int deviceNum);

	/**
	 * Get device chip model of specified SID
	 * 
	 * @param deviceNum SID device number
	 * 
	 * @return device chip model of specified SID (null means unassigned)
	 */
	ChipModel getDeviceChipModel(int deviceNum);
}
