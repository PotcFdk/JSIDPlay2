package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "musicInfo")
public class SongNoBean {

	public SongNoBean() {
	}
	
	public SongNoBean(int songNo) {
		this.songNo = songNo;
	}
	
	private int songNo;

	public int getSongNo() {
		return songNo;
	}

	@XmlElement(name = "songNo")
	public void setSongNo(int songNo) {
		this.songNo = songNo;
	}

}