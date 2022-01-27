package libsidplay.config;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IAudioSystemProperties {

	/**
	 * Video streaming: Time gap between emulation time and real time of the
	 * SleepDriver in ms.
	 */
	int MAX_TIME_GAP = Integer.valueOf(System.getProperty("jsidplay2.sleep_driver.max_time_gap", "10000"));

}
