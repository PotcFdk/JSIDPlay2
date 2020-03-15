package sidplay.audio.whatssid.fingerprint;

/**
 * Created by hsyecheng on 2015/6/12.
 */
public class Hash {
	public static int hash(Link link) {
		int dt = link.getEnd().getIntTime() - link.getStart().getIntTime(); //
		int df = link.getEnd().getIntFreq() - link.getStart().getIntFreq() + 300; // 300
		int freq = link.getStart().getIntFreq(); // 5000

		return freq + 5000 * (df + 600 * dt);
	}

	public static int[] hash2link(int hash) {
		int freq = hash % 5000;
		int df = (hash / 5000) % 600;
		int dt = hash / 5000 / 600;

		return new int[] { freq, df, dt };
	}
}
