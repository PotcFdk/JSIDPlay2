package sidplay.consoleplayer;

import sidplay.audio.AudioDriver;

public class DriverSettings {
	/** Default SID emulation */
	protected Emulation emulation = Emulation.EMU_RESID;
	/** Default output */
	protected Output output = Output.OUT_SOUNDCARD;
	/** Number of output channels */
	protected int channels;

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

	public int getChannels() {
		return channels;
	}

	public void setChannels(int channels) {
		this.channels = channels;
	}

	public final AudioDriver getDevice() {
		return output.getDriver();
	}
}