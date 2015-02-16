package libsidplay.common;

public enum Engine {
	/** No emulation. */
	NONE(""),

	/** Hardware */
	HARDSID("HardSID4U"),

	EMULATION("Emulation");

	private final String description;

	Engine(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}

}
