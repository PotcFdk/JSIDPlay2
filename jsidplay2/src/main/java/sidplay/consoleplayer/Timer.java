package sidplay.consoleplayer;

public class Timer {
	private long start;
	private long current;
	private long stop;
	private long defaultLength; // 0 - FOREVER
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