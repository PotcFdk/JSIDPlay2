package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "id")
public class IdBean {

	private int id;

	public IdBean() {
	}

	public IdBean(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@XmlElement(name = "id")
	public void setId(int id) {
		this.id = id;
	}

}
