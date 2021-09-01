package server.restful.servlets;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IConvertServletSystemProperties {

	/**
	 * Maximum number of video creation threads at the same time.
	 */
	int MAX_RTMP_THREADS = Integer.valueOf(System.getProperty("rtmp.max_threads", "3"));

	/**
	 * Time gap between emulation time and real time of the video production thread
	 * in ms.
	 */
	int MAX_TIME_GAP = Integer.valueOf(System.getProperty("rtmp.max_time_gap", "1000"));

	/**
	 * Interval between simulated key presses of the space key in s (required to
	 * watch some demos).
	 */
	int PRESS_SPACE_INTERVALL = Integer.valueOf(System.getProperty("rtmp.press_space_intervall", "40"));

	/**
	 * Live streaming upload url for the video creation process.
	 */
	String RTMP_UPLOAD_URL = System.getProperty("rtmp.upload.url", "rtmp://localhost/live");

	/**
	 * Live streaming download url for the video player for requests from the
	 * internet.
	 */
	String RTMP_EXTERNAL_DOWNLOAD_URL = System.getProperty("rtmp.external.download.url",
			"rtmp://haendel.ddns.net/live");

	/**
	 * Live streaming download url for the video player for requests from inside the
	 * internal network.
	 */
	String RTMP_INTERNAL_DOWNLOAD_URL = System.getProperty("rtmp.internal.download.url",
			"rtmp://haendel.ddns.net/live");

}
