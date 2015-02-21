package libsidplay.common;

public enum Engine {

	/** Hardware */
	HARDSID("HardSID4U"),

	/** Software */
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
