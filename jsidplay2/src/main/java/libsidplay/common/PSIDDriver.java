package libsidplay.common;

public enum PSIDDriver {
	/** Java SIDPlay2 PSID driver */
	JSIDPLAY2("/libsidplay/sidtune/psiddriver.asm"),
	/** VICE PSID driver */
	VICE("/libsidplay/sidtune/psiddriver_vice.asm");

	private final String driverPath;

	private PSIDDriver(String driverPath) {
		this.driverPath = driverPath;
	}

	public final String getDriverPath() {
		return driverPath;
	}
}
