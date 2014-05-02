package resid_builder.resid;

public enum SamplingMethod {
	DECIMATE("Decimate"), RESAMPLE("Resample");

	final String description;

	SamplingMethod(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}
}