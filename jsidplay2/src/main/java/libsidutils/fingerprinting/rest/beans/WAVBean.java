package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "wav")
public class WAVBean {

	private byte[] wav;

	public WAVBean() {
	}

	public WAVBean(byte[] wav) {
		this.wav = wav;
	}

	public byte[] getWav() {
		return wav;
	}

	@XmlElement(name = "wav")
	public void setWav(byte[] wav) {
		this.wav = wav;
	}

}
