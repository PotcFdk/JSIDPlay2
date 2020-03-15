package sidplay.audio.whatssid.fingerprint;

public class Link {

	private final Peak start, end;
	private final float[] tmp = new float[3];

	public Link(Peak s, Peak e) {
		this.start = s;
		this.end = e;
		this.tmp[0] = s.getIntFreq();
		this.tmp[1] = e.getIntFreq();
		this.tmp[2] = e.getIntTime() - s.getIntTime();
	}

	public Peak getStart() {
		return start;
	}

	public Peak getEnd() {
		return end;
	}
}