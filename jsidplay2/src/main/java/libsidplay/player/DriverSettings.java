package libsidplay.player;

import libsidplay.common.Emulation;
import sidplay.audio.Audio;

public class DriverSettings {
	/** SID emulation */
	private final Emulation emulation;
	/** output */
	private final Audio audio;

	public DriverSettings(final Audio audio, final Emulation emulation) {
		this.audio = audio;
		this.emulation = emulation;
	}

	public final Emulation getEmulation() {
		return emulation;
	}

	public final Audio getAudio() {
		return audio;
	}

}