package sidplay.consoleplayer;

import java.io.File;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import sidplay.audio.CmpMP3File;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;

public class DriverSettings {
	/** Default SID emulation */
	protected Emulation emulation = Emulation.EMU_RESID;
	/** Default output */
	protected Output output = Output.OUT_SOUNDCARD;
	/** Number of output channels */
	private int channels;
	private Integer secondAddress;

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

	public Integer getSecondAddress() {
		return secondAddress;
	}

	/** Determine number of SIDs */
	public void configure(IConfig iniCfg, SidTune tune, Player player) {
		SidTuneInfo tuneInfo = tune != null && tune.getInfo() != null ? tune
				.getInfo() : null;
		secondAddress = null;
		this.channels = 1;
		if (iniCfg.getEmulation().isForceStereoTune()) {
			secondAddress = iniCfg.getEmulation().getDualSidBase();
		} else if (tuneInfo != null && tuneInfo.sidChipBase2 != 0) {
			secondAddress = tuneInfo.sidChipBase2;
		}
		if (secondAddress != null) {
			this.channels = 2;
			if (secondAddress != 0xd400) {
				player.getC64().setSecondSIDAddress(secondAddress);
			}
		}

		if (output.getDriver() instanceof CmpMP3File) {
			// Set MP3 comparison settings
			IAudioSection audio = iniCfg.getAudio();
			CmpMP3File cmpMp3Driver = (CmpMP3File) output.getDriver();
			cmpMp3Driver.setPlayOriginal(audio.isPlayOriginal());
			cmpMp3Driver.setMp3File(new File(audio.getMp3File()));
		}
	}

	public DriverSettings save() {
		DriverSettings newSettings = new DriverSettings();
		newSettings.output = this.output;
		newSettings.emulation = this.emulation;
		newSettings.channels = this.channels;
		newSettings.secondAddress = this.secondAddress;
		return newSettings;
	}

	public void restore(DriverSettings oldDriverSettings) {
		this.output = oldDriverSettings.output;
		this.emulation = oldDriverSettings.emulation;
		this.channels = oldDriverSettings.channels;
		this.secondAddress = oldDriverSettings.secondAddress;
	}
}