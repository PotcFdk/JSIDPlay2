package libsidplay.player;

import libsidplay.common.Emulation;
import sidplay.audio.AudioDriver;

public class DriverSettings {
	/** SID emulation */
	private final Emulation emulation;
	/** output */
	private final AudioDriver driver;

	public DriverSettings(final AudioDriver driver, final Emulation emulation) {
		this.driver = driver;
		this.emulation = emulation;
	}

	public final Emulation getEmulation() {
		return emulation;
	}

	public final AudioDriver getAudioDriver() {
		return driver;
	}

}