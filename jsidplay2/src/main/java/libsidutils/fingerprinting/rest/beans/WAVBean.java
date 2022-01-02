package libsidutils.fingerprinting.rest.beans;

import static libsidplay.config.IWhatsSidSystemProperties.FRAME_MAX_LENGTH;

import java.util.Arrays;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((wav == null) ? 0 : Arrays.hashCode(wav));
		result = prime * result + Long.hashCode(frameMaxLength);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WAVBean)) {
			return false;
		}
		WAVBean other = (WAVBean) obj;
		return Arrays.equals(wav, other.wav) && frameMaxLength == other.frameMaxLength;
	}
}
