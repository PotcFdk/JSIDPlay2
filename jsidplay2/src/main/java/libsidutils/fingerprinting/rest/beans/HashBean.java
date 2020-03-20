package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "hash")
@XmlType(propOrder = { "hash", "id", "time" })
public class HashBean {

	private int hash;
	private int id;
	private int time;

	public int getHash() {
		return hash;
	}

	@XmlElement(name = "hash")
	public void setHash(int hash) {
		this.hash = hash;
	}

	public int getId() {
		return id;
	}

	@XmlElement(name = "id")
	public void setId(int id) {
		this.id = id;
	}

	public int getTime() {
		return time;
	}

	@XmlElement(name = "time")
	public void setTime(int time) {
		this.time = time;
	}

}
