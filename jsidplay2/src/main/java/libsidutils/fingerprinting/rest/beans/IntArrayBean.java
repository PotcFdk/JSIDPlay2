package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ints")
public class IntArrayBean {

	private int[] hash;

	public int[] getHash() {
		return hash;
	}

	@XmlElement(name = "hash")
	public void setHash(int[] hash) {
		this.hash = hash;
	}

}
