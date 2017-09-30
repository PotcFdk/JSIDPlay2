package common;

public interface Timeouts {
	/**
	 * Sleep time to wait for JSIDPlay2 to start.
	 */
	int JSIDPLAY2_STARTUP_SLEEP = 1000;
	/**
	 * Timeout for the filebrowser to open.
	 */
	int FILE_BROWSER_OPENED_TIMEOUT = 2000;
	/**
	 * Timeout until the C64 has been reset completely.
	 */
	int C64_RESET_TIMEOUT = 2500;
	/**
	 * Timeout for a thread-safe scheduled event.
	 */
	int SCHEDULE_THREADSAFE_TIMEOUT = 2000;
}
