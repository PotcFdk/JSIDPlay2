package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "wav")
public class WavBean {

	public WavBean() {
	}

	public WavBean(byte[] wavData) {
		this.wavData = wavData;
	}

	private byte[] wavData;

	public byte[] getWavData() {
		return wavData;
	}

	@XmlElement(name = "wav")
	public void setWavData(byte[] wavData) {
		this.wavData = wavData;
	}

}
