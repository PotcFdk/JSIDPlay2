package libsidutils.fingerprinting.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "hashes")
public class HashBeans {

	private List<HashBean> hashes;

	public List<HashBean> getHashes() {
		return hashes;
	}

	@XmlElementWrapper
	@XmlElement(name = "hash")
	public void setHashes(List<HashBean> hashBeans) {
		this.hashes = hashBeans;
	}

}
