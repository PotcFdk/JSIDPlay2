package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "hashes")
public class IntArrayBean {

	private Integer[] hash;

	public IntArrayBean() {
	}

	public IntArrayBean(Integer[] hash) {
		this.hash = hash;
	}

	public Integer[] getHash() {
		return hash;
	}

	@XmlElement(name = "hash")
	public void setHash(Integer[] hash) {
		this.hash = hash;
	}

}
