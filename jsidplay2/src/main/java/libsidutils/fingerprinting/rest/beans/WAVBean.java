package libsidutils.fingerprinting.rest.beans;

import static libsidplay.config.IWhatsSidSystemProperties.FRAME_MAX_LENGTH;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "wav")
public class WAVBean {

	private byte[] wav;

	private long frameMaxLength = FRAME_MAX_LENGTH;

	public WAVBean() {
	}

	public WAVBean(byte[] wav) {
		this.wav = wav;
	}

	public WAVBean(byte[] wav, long frameMaxLength) {
		this.wav = wav;
		this.frameMaxLength = frameMaxLength;
	}

	public byte[] getWav() {
		return wav;
	}

	@XmlElement(name = "wav")
	public void setWav(byte[] wav) {
		this.wav = wav;
	}

	public long getFrameMaxLength() {
		return frameMaxLength;
	}

	@XmlElement(name = "frameMaxLength")
	public void setFrameMaxLength(long frameMaxLength) {
		this.frameMaxLength = frameMaxLength;
	}

}
