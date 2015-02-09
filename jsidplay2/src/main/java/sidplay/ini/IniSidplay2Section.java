package sidplay.ini;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.ISidPlay2Section;

/**
 * SIDPlay2 section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniSidplay2Section extends IniSection implements ISidPlay2Section {

	/**
	 * SIDPlay2 section of the INI file.
	 * 
	 * @param ini
	 *            INI file reader
	 */
	protected IniSidplay2Section(final IniReader ini) {
		super(ini);
	}

	/**
	 * Get INI file version.
	 * 
	 * @return INI file version
	 */
	@Override
	public final int getVersion() {
		return iniReader.getPropertyInt("SIDPlay2", "Version",
				IConfig.REQUIRED_CONFIG_VERSION);
	}

	@Override
	public void setVersion(int version) {
		/* Set the current version so that we detect old versions in future. */
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

	public boolean isLoop() {
		return iniReader.getPropertyBool("SIDPlay2", "Loop", DEFAULT_LOOP);
	}

	public void setLoop(boolean loop) {
		iniReader.setProperty("SIDPlay2", "Loop", loop);
	}

	/**
	 * Do we play a single song per tune?
	 * 
	 * @return play a single song per tune
	 */
	@Override
	public final boolean isSingle() {
		return iniReader.getPropertyBool("SIDPlay2", "SingleTrack",
				DEFAULT_SINGLE_TRACK);
	}

	/**
	 * setter to play a single song per tune.
	 * 
	 * @param singleSong
	 *            play a single song per tune
	 */
	@Override
	public final void setSingle(final boolean singleSong) {
		iniReader.setProperty("SIDPlay2", "SingleTrack", singleSong);
	}

	/**
	 * Getter of the HVSC collection directory.
	 * 
	 * @return the HVSC collection directory
	 */
	@Override
	public final String getHvsc() {
		return iniReader.getPropertyString("SIDPlay2", "HVSC Dir", null);
	}

	/**
	 * Setter of the HVSC collection directory.
	 * 
	 * @param hvsc
	 *            the HVSC collection directory
	 */
	@Override
	public final void setHvsc(final String hvsc) {
		iniReader.setProperty("SIDPlay2", "HVSC Dir", hvsc);
	}

	/**
	 * Getter of the last accessed directory in the file browser.
	 * 
	 * @return the last accessed directory in the file browser
	 */
	@Override
	public final String getLastDirectory() {
		return iniReader.getPropertyString("SIDPlay2", "Last Directory", null);
	}

	/**
	 * Setter of the last accessed directory in the file browser.
	 * 
	 * @param lastDir
	 *            the last accessed directory in the file browser
	 */
	@Override
	public final void setLastDirectory(final String lastDir) {
		iniReader.setProperty("SIDPlay2", "Last Directory", lastDir);
	}

	/**
	 * Getter of the temporary directory for JSIDPlay2.
	 * 
	 * Default is <homeDir>/.jsidplay2
	 * 
	 * @return the temporary directory for JSIDPlay2
	 */
	@Override
	public final String getTmpDir() {
		return iniReader.getPropertyString(
				"SIDPlay2",
				"Temp Dir",
				System.getProperty("user.home")
						+ System.getProperty("file.separator") + ".jsidplay2");
	}

	/**
	 * Setter of the temporary directory for JSIDPlay2.
	 * 
	 * @param path
	 *            the temporary directory for JSIDPlay2
	 */
	@Override
	public final void setTmpDir(final String path) {
		iniReader.setProperty("SIDPlay2", "Temp Dir", path);
	}

}