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
	 * Time gap between emulation time and real time of the SleepDriver in ms.
	 */
	int MAX_TIME_GAP = Integer.valueOf(System.getProperty("jsidplay2.sleep_driver.max_time_gap", "10000"));

	/**
	 * Maximum length in seconds the video generation process is running.
	 */
	int MAX_LENGTH = Integer.valueOf(System.getProperty("jsidplay2.rtmp.max_seconds", "600"));

	/**
	 * Interval between simulated key presses of the space key in s (required to
	 * watch some demos).
	 */
	int PRESS_SPACE_INTERVALL = Integer.valueOf(System.getProperty("jsidplay2.rtmp.press_space_intervall", "90"));

	/**
	 * Live streaming upload url for the video creation process.
	 */
	String RTMP_UPLOAD_URL = System.getProperty("jsidplay2.rtmp.upload.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Live streaming download url for the video player for requests from the
	 * internet.
	 */
	String RTMP_EXTERNAL_DOWNLOAD_URL = System.getProperty("jsidplay2.rtmp.external.download.url",
			"rtmp://haendel.ddns.net/live");

	/**
	 * Live streaming download url for the video player for requests from inside the
	 * internal network.
	 */
	String RTMP_INTERNAL_DOWNLOAD_URL = System.getProperty("jsidplay2.rtmp.internal.download.url",
			"rtmp://haendel.ddns.net/live");

}
