package sidplay.audio.whatssid.database;

import sidplay.audio.whatssid.fingerprint.Fingerprint;
import sidplay.audio.whatssid.fingerprint.Hash;

public class Info {
	public final int hash;
	public final int id;
	public final int time;

	public Info(int id, Fingerprint.Link link) {
		this.id = id;
		this.time = link.start.intTime;
		this.hash = Hash.hash(link);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}