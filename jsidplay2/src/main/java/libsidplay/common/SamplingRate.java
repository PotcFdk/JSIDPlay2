package libsidplay.common;

public enum SamplingRate {
	/** 8 KHz */
	VERY_LOW(8000, 3600),
	/** 44.1 KHz */
	LOW(44100, 20000),
	/** 48 KHz */
	MEDIUM(48000, 20000),
	/** 96 KHz */
	HIGH(96000, 20000);

	private final int frequency, middleFrequency;

	private SamplingRate(final int frequency, final int middleFrequency) {
		this.frequency = frequency;
		this.middleFrequency = middleFrequency;
	}

	public int getFrequency() {
		return frequency;
	}

	public int getMiddleFrequency() {
		return middleFrequency;
	}
}
