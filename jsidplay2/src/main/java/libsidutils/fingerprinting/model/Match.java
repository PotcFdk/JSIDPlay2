package libsidutils.fingerprinting.model;

public class Match {

	private int count, time;

	public Match(int count, int time) {
		this.count = count;
		this.time = time;
	}

	public int getCount() {
		return count;
	}

	public void updateCount() {
		this.count++;
	}

	public int getTime() {
		return time;
	}
}