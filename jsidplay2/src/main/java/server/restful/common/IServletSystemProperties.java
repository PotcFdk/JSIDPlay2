package server.restful.common;

import static java.lang.String.valueOf;
import static libsidplay.common.SamplingRate.VERY_LOW;

public interface IServletSystemProperties {

	//
	// JSIDPlay2Server
	//

	/**
	 * JSIDPlay2Server Socket connection timeout in ms of the HTTP(s) connection.
	 */
	int CONNECTION_TIMEOUT = Integer
			.valueOf(System.getProperty("jsidplay2.whatssid.connection.timeout", valueOf(120/* s */ * 1000)));

	//
	// ConvertServlet
	//

	/**
	 * Video streaming: Interval between simulated key presses of the space key in s
	 * (required to watch some demos).
	 */
	int PRESS_SPACE_INTERVALL = Integer.valueOf(System.getProperty("jsidplay2.rtmp.press_space_intervall", "90"));

	/**
	 * Video streaming: Upload url for the video creation process.
	 */
	String RTMP_UPLOAD_URL = System.getProperty("jsidplay2.rtmp.upload.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: Download url for the video player for requests from the
	 * internet.
	 */
	String RTMP_EXTERNAL_DOWNLOAD_URL = System.getProperty("jsidplay2.rtmp.external.download.url",
			"rtmp://haendel.ddns.net/live"/* http://haendel.ddns.net:90/hls */);

	/**
	 * Video streaming: Download url for the video player for requests from inside
	 * the internal network.
	 */
	String RTMP_INTERNAL_DOWNLOAD_URL = System.getProperty("jsidplay2.rtmp.internal.download.url",
			"rtmp://haendel.ddns.net/live");

	/**
	 * Video download: Maximum length in seconds the video download process is
	 * running.
	 */
	int MAX_LENGTH = Integer.valueOf(System.getProperty("jsidplay2.rtmp.max_seconds", "600"));

	//
	// WhatsSIDServlet
	//

	/**
	 * WhatsSID? Maximum number of audio frames used to recognize a tune for file
	 * upload.
	 */
	long FRAME_MAX_LENGTH_UPLOAD = Long.valueOf(System.getProperty("jsidplay2.whatssid.upload.frame.max.length",
			valueOf(120/* s */ * VERY_LOW.getFrequency())));

	/**
	 * WhatsSID? Cache size. Recognized audio is cached for performance reasons.
	 */
	int CACHE_SIZE = Integer.valueOf(System.getProperty("jsidplay2.whatssid.cache.size", valueOf(60000)));
}