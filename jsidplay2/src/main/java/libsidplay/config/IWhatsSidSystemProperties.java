package libsidplay.config;

import static java.lang.String.valueOf;
import static libsidplay.common.SamplingRate.VERY_LOW;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IWhatsSidSystemProperties {

	/**
	 * WhatsSID? Maximum number of audio frames used to recognize a tune.
	 */
	long FRAME_MAX_LENGTH = Long.valueOf(
			System.getProperty("jsidplay2.whatssid.frame.max.length", valueOf(15/* s */ * VERY_LOW.getFrequency())));

	/**
	 * WhatsSID? Maximum number of audio frames used to recognize a tune for file
	 * upload.
	 */
	long FRAME_MAX_LENGTH_UPLOAD = Long.valueOf(System.getProperty("jsidplay2.whatssid.upload.frame.max.length",
			valueOf(120/* s */ * VERY_LOW.getFrequency())));

	/**
	 * WhatsSID? Socket connection timeout in ms of the HTTP(s) connection.
	 */
	int CONNECTION_TIMEOUT = Integer
			.valueOf(System.getProperty("jsidplay2.whatssid.connection.timeout", valueOf(120/* s */ * 1000)));

	/**
	 * WhatsSID? query timeout in ms of tune recognition's findHashes query to
	 * prevent blocking database connections during database startup.
	 */
	int QUERY_TIMEOUT = Integer
			.valueOf(System.getProperty("jsidplay2.whatssid.query.timeout", valueOf(30/* s */ * 1000)));

}
