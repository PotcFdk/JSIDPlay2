package libsidplay.player;

import sidplay.audio.Output;

public class DriverSettings {
	/** SID emulation */
	protected Emulation emulation;
	/** output */
	protected Output output;

	public DriverSettings(Output output, Emulation emulation) {
		this.emulation = emulation;
		this.output = output;
	}

	public final Emulation getEmulation() {
		return emulation;
	}

	public final void setEmulation(final Emulation emulation) {
		this.emulation = emulation;
	}

	public final Output getOutput() {
		return output;
	}

	public final void setOutput(final Output output) {
		this.output = output;
	}

	public DriverSettings save() {
		return new DriverSettings(this.output, this.emulation);
	}

	public void restore(DriverSettings oldDriverSettings) {
		this.output = oldDriverSettings.output;
		this.emulation = oldDriverSettings.emulation;
	}
}