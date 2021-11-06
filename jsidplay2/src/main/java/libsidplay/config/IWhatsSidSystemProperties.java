package libsidplay.config;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IWhatsSidSystemProperties {

	/**
	 * WhatsSID? Socket connection timeout of the HTTP(s) connection.
	 */
	int CONNECTION_TIMEOUT = Integer.valueOf(System.getProperty("jsidplay2.whatssid.connection.timeout", "120000"));

	/**
	 * WhatsSID? Maximum number of audio frames used to recognize a tune.
	 */
	long FRAME_MAX_LENGTH = Long.valueOf(System.getProperty("jsidplay2.whatssid.frame.max.length", "56000"));

	/**
	 * WhatsSID? Maximum number of audio frames used to recognize a tune for file
	 * upload.
	 */
	long FRAME_MAX_LENGTH_UPLOAD = Long
			.valueOf(System.getProperty("jsidplay2.whatssid.upload.frame.max.length", "960000"));

	/**
	 * WhatsSID? query timeout of tune recognition's findHashes query to prevent
	 * blocking database connections during database startup.
	 */
	int QUERY_TIMEOUT = Integer.valueOf(System.getProperty("jsidplay2.whatssid.query.timeout", "30000"));

}
