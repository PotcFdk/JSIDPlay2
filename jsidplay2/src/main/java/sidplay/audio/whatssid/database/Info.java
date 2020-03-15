package sidplay.audio.whatssid.database;

import sidplay.audio.whatssid.fingerprint.Hash;
import sidplay.audio.whatssid.fingerprint.Link;

public class Info {
	public final int hash;
	public final int id;
	public final int time;

	public Info(int id, Link link) {
		this.id = id;
		this.time = link.getStart().getIntTime();
		this.hash = Hash.hash(link);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}