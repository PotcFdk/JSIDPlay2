package sidplay.consoleplayer;

public class Timer {
	/**
	 * Start time, When do we start playing (normally 0).
	 */
	private long start;
	/**
	 * Current play time.
	 */
	private long current;
	/**
	 * Time, when a song ends (play time is over), relative to start time.
	 */
	private long stop;
	/**
	 * Play length, 0 means forever (if song length is unknown)
	 */
	private long defaultLength;
	/**
	 * Ignore song length information and use the defaultLength (true) or
	 * auto-detect song length
	 */
	private boolean valid;

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getCurrent() {
		return current;
	}

	public void setCurrent(long current) {
		this.current = current;
	}

	public long getStop() {
		return stop;
	}

	public void setStop(long stop) {
		this.stop = stop;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public long getDefaultLength() {
		return defaultLength;
	}

	public void setDefaultLength(final long length) {
		this.defaultLength = length;
	}
}