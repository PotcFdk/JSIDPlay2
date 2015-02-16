package libsidplay.player;

import sidplay.audio.AudioDriver;

public class DriverSettings {
	/** output */
	private final AudioDriver driver;

	public DriverSettings(final AudioDriver driver) {
		this.driver = driver;
	}

	public final AudioDriver getAudioDriver() {
		return driver;
	}

}