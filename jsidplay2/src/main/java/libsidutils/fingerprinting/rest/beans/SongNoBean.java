package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "songNo")
public class SongNoBean {

	private int songNo;

	public SongNoBean() {
	}

	public SongNoBean(int songNo) {
		this.songNo = songNo;
	}

	public int getSongNo() {
		return songNo;
	}

	@XmlElement(name = "songNo")
	public void setSongNo(int songNo) {
		this.songNo = songNo;
	}

}
