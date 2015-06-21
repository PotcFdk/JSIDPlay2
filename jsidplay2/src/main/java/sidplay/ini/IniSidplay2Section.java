package sidplay.ini;

import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;

/**
 * SIDPlay2 section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniSidplay2Section extends IniSection implements ISidPlay2Section {

	protected IniSidplay2Section(final IniReader ini) {
		super(ini);
	}

	@Override
	public final int getVersion() {
		return iniReader.getPropertyInt("SIDPlay2", "Version",
				IConfig.REQUIRED_CONFIG_VERSION);
	}

	@Override
	public void setVersion(int version) {
		iniReader.setProperty("SIDPlay2", "Version", version);
	}

	@Override
	public final boolean isEnableDatabase() {
		return iniReader.getPropertyBool("SIDPlay2", "EnableDatabase",
				DEFAULT_ENABLE_DATABASE);
	}

	@Override
	public final void setEnableDatabase(final boolean enable) {
		iniReader.setProperty("SIDPlay2", "EnableDatabase", enable);
	}

	@Override
	public final int getDefaultPlayLength() {
		return iniReader.getPropertyTime("SIDPlay2", "Default Play Length",
				DEFAULT_PLAY_LENGTH);
	}

	@Override
	public final void setDefaultPlayLength(final int playLength) {
		iniReader.setProperty("SIDPlay2", "Default Play Length", String.format(
				"%02d:%02d", (playLength / 60), (playLength % 60)));
	}

	@Override
	public boolean isLoop() {
		return iniReader.getPropertyBool("SIDPlay2", "Loop", DEFAULT_LOOP);
	}

	@Override
	public void setLoop(boolean loop) {
		iniReader.setProperty("SIDPlay2", "Loop", loop);
	}

	@Override
	public final boolean isSingle() {
		return iniReader.getPropertyBool("SIDPlay2", "SingleTrack",
				DEFAULT_SINGLE_TRACK);
	}

	@Override
	public final void setSingle(final boolean singleSong) {
		iniReader.setProperty("SIDPlay2", "SingleTrack", singleSong);
	}

	@Override
	public final String getHvsc() {
		return iniReader.getPropertyString("SIDPlay2", "HVSC Dir", null);
	}

	@Override
	public final void setHvsc(final String hvsc) {
		iniReader.setProperty("SIDPlay2", "HVSC Dir", hvsc);
	}

	@Override
	public final String getLastDirectory() {
		return iniReader.getPropertyString("SIDPlay2", "Last Directory", null);
	}

	@Override
	public final void setLastDirectory(final String lastDir) {
		iniReader.setProperty("SIDPlay2", "Last Directory", lastDir);
	}

	@Override
	public final String getTmpDir() {
		return iniReader.getPropertyString(
				"SIDPlay2",
				"Temp Dir",
				System.getProperty("user.home")
						+ System.getProperty("file.separator") + ".jsidplay2");
	}

	@Override
	public final void setTmpDir(final String path) {
		iniReader.setProperty("SIDPlay2", "Temp Dir", path);
	}

	@Override
	public boolean isTurboTape() {
		return iniReader.getPropertyBool("SIDPlay2", "TurboTape",
				DEFAULT_TURBO_TAPE);
	}

	@Override
	public void setTurboTape(boolean turboTape) {
		iniReader.setProperty("SIDPlay2", "TurboTape", turboTape);
	}

}