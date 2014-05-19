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
		return iniReader.getPropertyBool("SIDPlay2", "EnableDatabase", true);
	}

	@Override
	public final void setEnableDatabase(final boolean enable) {
		iniReader.setProperty("SIDPlay2", "EnableDatabase", enable);
	}

	@Override
	public final int getDefaultPlayLength() {
		return iniReader.getPropertyTime("SIDPlay2", "Default Play Length",
				3 * 60);
	}

	@Override
	public final void setDefaultPlayLength(final int playLength) {
		iniReader.setProperty("SIDPlay2", "Default Play Length", String.format(
				"%02d:%02d", (playLength / 60), (playLength % 60)));
	}

	public boolean isLoop() {
		return iniReader.getPropertyBool("SIDPlay2", "Loop", false);
	}

	public void setLoop(boolean loop) {
		iniReader.setProperty("SIDPlay2", "Loop", loop);
	}

	/**
	 * Getter of the HVMEC collection directory.
	 * 
	 * @return the HVMEC collection directory
	 */
	@Override
	public final String getHVMEC() {
		return iniReader.getPropertyString("SIDPlay2", "HVMEC Dir", null);
	}

	/**
	 * Setter of the HVMEC directory.
	 * 
	 * @param hvmec
	 *            the HVMEC directory
	 */
	@Override
	public final void setHVMEC(final String hvmec) {
		iniReader.setProperty("SIDPlay2", "HVMEC Dir", hvmec);
	}

	/**
	 * Getter of the CGSC collection directory.
	 * 
	 * @return the CGSC collection directory
	 */
	@Override
	public final String getDemos() {
		return iniReader.getPropertyString("SIDPlay2", "DEMOS Dir", null);
	}

	/**
	 * Setter of the Demos directory.
	 * 
	 * @param demos
	 *            the Demos directory
	 */
	@Override
	public final void setDemos(final String demos) {
		iniReader.setProperty("SIDPlay2", "DEMOS Dir", demos);
	}

	/**
	 * Getter of the Mags directory.
	 * 
	 * @return the Mags directory
	 */
	@Override
	public final String getMags() {
		return iniReader.getPropertyString("SIDPlay2", "MAGS Dir", null);
	}

	/**
	 * Setter of the Mags directory.
	 * 
	 * @param mags
	 *            the Mags directory
	 */
	@Override
	public final void setMags(final String mags) {
		iniReader.setProperty("SIDPlay2", "MAGS Dir", mags);
	}

	/**
	 * Getter of the CGSC collection directory.
	 * 
	 * @return the CGSC collection directory
	 */
	@Override
	public final String getCgsc() {
		return iniReader.getPropertyString("SIDPlay2", "CGSC Dir", null);
	}

	/**
	 * Setter of the CGSC collection directory.
	 * 
	 * @param cgsc
	 *            the CGSC collection directory
	 */
	@Override
	public final void setCgsc(final String cgsc) {
		iniReader.setProperty("SIDPlay2", "CGSC Dir", cgsc);
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
	 * Do we play a single song per tune?
	 * 
	 * @return play a single song per tune
	 */
	@Override
	public final boolean isSingle() {
		return iniReader.getPropertyBool("SIDPlay2", "SingleTrack", false);
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
	 * Do we enable proxy for SOASC downloads?
	 * 
	 * @return enable proxy for SOASC downloads
	 */
	@Override
	public final boolean isEnableProxy() {
		return iniReader.getPropertyBool("SIDPlay2", "EnableProxy", false);
	}

	/**
	 * Setter to enable proxy for SOASC downloads.
	 * 
	 * @param enable
	 *            enable proxy for SOASC downloads
	 */
	@Override
	public final void setEnableProxy(final boolean enable) {
		iniReader.setProperty("SIDPlay2", "EnableProxy", enable);
	}

	/**
	 * Getter of the proxy hostname for SOASC downloads.
	 * 
	 * @return the proxy hostname for SOASC downloads
	 */
	@Override
	public final String getProxyHostname() {
		return iniReader.getPropertyString("SIDPlay2", "ProxyHostname", null);
	}

	/**
	 * Setter of the proxy hostname for SOASC downloads.
	 * 
	 * @param hostname
	 *            the proxy hostname for SOASC downloads
	 */
	@Override
	public final void setProxyHostname(final String hostname) {
		iniReader.setProperty("SIDPlay2", "ProxyHostname", hostname);
	}

	/**
	 * Getter of the proxy port for SOASC downloads.
	 * 
	 * @return the proxy port for SOASC downloads
	 */
	@Override
	public final int getProxyPort() {
		return iniReader.getPropertyInt("SIDPlay2", "ProxyPort", 80);
	}

	/**
	 * Setter of the proxy port for SOASC downloads.
	 * 
	 * @param port
	 *            the proxy port for SOASC downloads
	 */
	@Override
	public final void setProxyPort(final int port) {
		iniReader.setProperty("SIDPlay2", "ProxyPort", port);
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

	/**
	 * Getter of the application window X position.
	 * 
	 * @return the application window X position
	 */
	@Override
	public final int getFrameX() {
		return iniReader.getPropertyInt("SIDPlay2", "Frame X", -1);
	}

	/**
	 * Setter of the application window X position.
	 * 
	 * @param x
	 *            application window X position
	 */
	@Override
	public final void setFrameX(final int x) {
		iniReader.setProperty("SIDPlay2", "Frame X", x);
	}

	/**
	 * Getter of the application window Y position.
	 * 
	 * @return the application window Y position
	 */
	@Override
	public final int getFrameY() {
		return iniReader.getPropertyInt("SIDPlay2", "Frame Y", -1);
	}

	/**
	 * Setter of the application window Y position.
	 * 
	 * @param y
	 *            application window Y position
	 */
	@Override
	public final void setFrameY(final int y) {
		iniReader.setProperty("SIDPlay2", "Frame Y", y);
	}

	/**
	 * Getter of the application window width.
	 * 
	 * @return the application window width
	 */
	@Override
	public final int getFrameWidth() {
		return iniReader.getPropertyInt("SIDPlay2", "Frame Width", -1);
	}

	/**
	 * Setter of the application window width.
	 * 
	 * @param width
	 *            application window width
	 */
	@Override
	public final void setFrameWidth(final int width) {
		iniReader.setProperty("SIDPlay2", "Frame Width", width);
	}

	/**
	 * Getter of the application window height.
	 * 
	 * @return the application window height
	 */
	@Override
	public final int getFrameHeight() {
		return iniReader.getPropertyInt("SIDPlay2", "Frame Height", -1);
	}

	/**
	 * Setter of the application window height.
	 * 
	 * @param height
	 *            application window height
	 */
	@Override
	public final void setFrameHeight(final int height) {
		iniReader.setProperty("SIDPlay2", "Frame Height", height);
	}

}