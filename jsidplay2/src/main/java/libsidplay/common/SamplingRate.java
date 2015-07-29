package libsidplay.common;

public enum SamplingRate {
	/** 44.1 KHz */
	LOW(44100),
	/** 48 KHz */
	MEDIUM(48000),
	/** 96 KHz */
	HIGH(96000);

	private int frequency;

	private SamplingRate(int frequency) {
		this.frequency = frequency;
	}

	public int getFrequency() {
		return frequency;
	}
}
