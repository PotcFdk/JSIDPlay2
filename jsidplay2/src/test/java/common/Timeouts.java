package common;

public interface Timeouts {
	/**
	 * Timeout for JSIDPlay2 to start.
	 */
	int JSIDPLAY2_STARTUP_TIMEOUT = 20000;
	/**
	 * Timeout for the filebrowser to open.
	 */
	int FILE_BROWSER_OPENED_TIMEOUT = 2000;
	/**
	 * Timeout for the SID tune to load.
	 */
	int SID_TUNE_LOADED_TIMEOUT = 5000;
	/**
	 * Timeout for a thread-safe scheduled event.
	 */
	int SCHEDULE_THREADSAFE_TIMEOUT = 2000;
}
