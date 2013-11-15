package netsiddev;

import javax.sound.sampled.Mixer.Info;

class AudioDevice {
	private Info info;
	private Integer deviceIndex;

	public Info getInfo() {
		return info;
	}

	public Integer getIndex() {
		return deviceIndex;
	}

	public AudioDevice(final Integer index, final Info info) {
		this.deviceIndex = index;
		this.info = info;
	}

	@Override
	public String toString() {
		return info.getName();
	}
}