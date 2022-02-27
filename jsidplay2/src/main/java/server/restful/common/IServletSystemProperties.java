package server.restful.common;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;

public interface IServletSystemProperties {

	//
	// JSIDPlay2Server
	//

	/**
	 * JSIDPlay2Server Socket connection timeout in s of the HTTP(s) connection.
	 */
	int CONNECTION_TIMEOUT = valueOf(getProperty("jsidplay2.whatssid.connection.timeout", "120"));

	//
	// ConvertServlet
	//

	/**
	 * WhatsSID? Maximum number of requests in parallel.
	 */
	int MAX_CONVERT_IN_PARALLEL = valueOf(getProperty("jsidplay2.convert.max.parallel", "7"));

	/**
	 * WhatsSID? Maximum number of RTMP threads in parallel.
	 */
	int MAX_RTMP_IN_PARALLEL = valueOf(getProperty("jsidplay2.rtmp.max.parallel", "7"));

	/**
	 * Video streaming: Interval between simulated key presses of the space key in s
	 * (required to watch some demos).
	 */
	int PRESS_SPACE_INTERVALL = valueOf(getProperty("jsidplay2.rtmp.press_space_intervall", "90"));

	/**
	 * Time span to wait until RTMP is available after requesting it.
	 */
	int WAIT_FOR_RTMP = valueOf(getProperty("jsidplay2.rtmp.wait.for", "1000"));

	/**
	 * Video streaming: Live stream created but not yet played will be quit after
	 * timeout in s.
	 */
	int RTMP_NOT_YET_PLAYED_TIMEOUT = valueOf(getProperty("jsidplay2.rtmp.not_yet_played.timeout", "20"));

	/**
	 * Video streaming: Live stream played and exceeds maximum duration will be quit
	 * after timeout in s.
	 */
	int RTMP_EXCEEDS_MAXIMUM_DURATION = valueOf(getProperty("jsidplay2.rtmp.exceeds_maximum.duration", "3600"));

	/**
	 * Video streaming: Time in s to print current RTMP video players.
	 */
	int RTMP_CLEANUP_PLAYER_COUNTER = valueOf(getProperty("jsidplay2.rtmp.cleanup.player.period", "30"));

	/**
	 * Video streaming: Upload url for the video creation process.
	 */
	String RTMP_UPLOAD_URL = getProperty("jsidplay2.rtmp.upload.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: Download url for the video player for requests from the
	 * internet.
	 */
	String RTMP_EXTERNAL_DOWNLOAD_URL = getProperty("jsidplay2.rtmp.external.download.url",
			"rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: Download url for the video player for requests from inside
	 * the internal network.
	 */
	String RTMP_INTERNAL_DOWNLOAD_URL = getProperty("jsidplay2.rtmp.internal.download.url",
			"rtmp://haendel.ddns.net/live");

	/**
	 * Video download: Maximum length in seconds the video download process is
	 * running.
	 */
	int MAX_LENGTH = valueOf(getProperty("jsidplay2.rtmp.max_seconds", "600"));

	//
	// WhatsSIDServlet
	//

	/**
	 * WhatsSID? Maximum number of requests in parallel.
	 */
	int MAX_WHATSIDS_IN_PARALLEL = valueOf(getProperty("jsidplay2.whatssid.max.parallel", "7"));

	/**
	 * WhatsSID? Maximum duration used to recognize a tune for file upload.
	 */
	int UPLOAD_MAXIMUM_DURATION = valueOf(getProperty("jsidplay2.whatssid.upload.max.duration", "120"));

	/**
	 * WhatsSID? Cache size. Recognized audio is cached for performance reasons.
	 */
	int CACHE_SIZE = valueOf(getProperty("jsidplay2.whatssid.cache.size", "60000"));

}