package libsidutils.fingerprinting.database;

import libsidutils.fingerprinting.fingerprint.Hash;
import libsidutils.fingerprinting.fingerprint.Link;

public class Info {
	public final int hash, id, time;

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