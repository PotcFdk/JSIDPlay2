package libsidplay;

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
	 * Time, when a song ends, relative to start time.
	 */
	private long stop;

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

}