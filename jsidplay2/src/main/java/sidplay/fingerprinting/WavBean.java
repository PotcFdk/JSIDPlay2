package sidplay.fingerprinting;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "wav")
public class WavBean {

	public WavBean() {
	}

	public WavBean(byte[] wav) {
		this.wav = wav;
	}

	private byte[] wav;

	public byte[] getWav() {
		return wav;
	}

	@XmlElement(name = "wav")
	public void setWav(byte[] wav) {
		this.wav = wav;
	}

}
