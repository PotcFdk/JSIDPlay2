package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "hashes")
public class IntArrayBean {

	public IntArrayBean() {
	}

	public IntArrayBean(Integer[] hash) {
		this.hash = hash;
	}

	private Integer[] hash;

	public Integer[] getHash() {
		return hash;
	}

	@XmlElement(name = "hash")
	public void setHash(Integer[] hash) {
		this.hash = hash;
	}

}
