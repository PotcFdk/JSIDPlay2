package libsidplay.common;

public enum Emulation {
	/** No emulation. */
	NONE(""),
	/** Dag Lem's resid 1.0 beta */
	RESID("Dag Lem's resid 1.0 beta"),
	/** Antti S. Lankila's resid-fp */
	RESIDFP("Antti S. Lankila's resid-fp"),
	/** Hardware */
	HARDSID("HardSID Hardware");
	
	private final String description;

	Emulation(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}
}