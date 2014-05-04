package sidplay.ini.intf;


public interface ISidPlay2Section {

	/**
	 * Get INI file version.
	 * 
	 * @return INI file version
	 */
	public int getVersion();

	/**
	 * Set configuration version
	 * 
	 * @param version
	 *            configuration version
	 */
	public void setVersion(int version);

	/**
	 * Getter of the enable of the Songlengths database.
	 * 
	 * @return Is the Songlengths database enabled?
	 */
	public boolean isEnableDatabase();

	/**
	 * Setter of the enable of the Songlengths database.
	 * 
	 * @param enable
	 *            the enable of the Songlengths database
	 */
	public void setEnableDatabase(boolean enable);

	/**
	 * Getter of the user defined fixed play length.
	 * 
	 * @return default play length
	 */
	public int getUserPlayLength();

	/**
	 * Setter of the user defined fixed play length.
	 * 
	 * @param playLength
	 *            default play length
	 */
	public void setUserPlayLength(int playLength);

	/**
	 * Getter of the default play length (if the song length is unknown).
	 * 
	 * @return default play length
	 */
	public int getDefaultPlayLength();

	/**
	 * Setter of the default play length (if the song length is unknown).
	 * 
	 * @param playLength
	 *            default play length
	 */
	public void setDefaultPlayLength(int playLength);

	public boolean isLoop();

	public void setLoop(boolean loop);

	/**
	 * Getter of the HVMEC collection directory.
	 * 
	 * @return the HVMEC collection directory
	 */
	public String getHVMEC();

	/**
	 * Setter of the HVMEC directory.
	 * 
	 * @param hvmec
	 *            the HVMEC directory
	 */
	public void setHVMEC(String hvmec);

	/**
	 * Getter of the CGSC collection directory.
	 * 
	 * @return the CGSC collection directory
	 */
	public String getDemos();

	/**
	 * Setter of the Demos directory.
	 * 
	 * @param demos
	 *            the Demos directory
	 */
	public void setDemos(String demos);

	/**
	 * Getter of the Mags directory.
	 * 
	 * @return the Mags directory
	 */
	public String getMags();

	/**
	 * Setter of the Mags directory.
	 * 
	 * @param mags
	 *            the Mags directory
	 */
	public void setMags(String mags);

	/**
	 * Getter of the CGSC collection directory.
	 * 
	 * @return the CGSC collection directory
	 */
	public String getCgsc();

	/**
	 * Setter of the CGSC collection directory.
	 * 
	 * @param cgsc
	 *            the CGSC collection directory
	 */
	public void setCgsc(String cgsc);

	/**
	 * Getter of the HVSC collection directory.
	 * 
	 * @return the HVSC collection directory
	 */
	public String getHvsc();

	/**
	 * Setter of the HVSC collection directory.
	 * 
	 * @param hvsc
	 *            the HVSC collection directory
	 */
	public void setHvsc(String hvsc);

	/**
	 * Do we play a single song per tune?
	 * 
	 * @return play a single song per tune
	 */
	public boolean isSingle();

	/**
	 * setter to play a single song per tune.
	 * 
	 * @param singleSong
	 *            play a single song per tune
	 */
	public void setSingle(boolean singleSong);

	/**
	 * Do we enable proxy for SOASC downloads?
	 * 
	 * @return enable proxy for SOASC downloads
	 */
	public boolean isEnableProxy();

	/**
	 * Setter to enable proxy for SOASC downloads.
	 * 
	 * @param enable
	 *            enable proxy for SOASC downloads
	 */
	public void setEnableProxy(boolean enable);

	/**
	 * Getter of the proxy hostname for SOASC downloads.
	 * 
	 * @return the proxy hostname for SOASC downloads
	 */
	public String getProxyHostname();

	/**
	 * Setter of the proxy hostname for SOASC downloads.
	 * 
	 * @param hostname
	 *            the proxy hostname for SOASC downloads
	 */
	public void setProxyHostname(String hostname);

	/**
	 * Getter of the proxy port for SOASC downloads.
	 * 
	 * @return the proxy port for SOASC downloads
	 */
	public int getProxyPort();

	/**
	 * Setter of the proxy port for SOASC downloads.
	 * 
	 * @param port
	 *            the proxy port for SOASC downloads
	 */
	public void setProxyPort(int port);

	/**
	 * Getter of the last accessed directory in the file browser.
	 * 
	 * @return the last accessed directory in the file browser
	 */
	public String getLastDirectory();

	/**
	 * Setter of the last accessed directory in the file browser.
	 * 
	 * @param lastDir
	 *            the last accessed directory in the file browser
	 */
	public void setLastDirectory(String lastDir);

	/**
	 * Getter of the temporary directory for JSIDPlay2.
	 * 
	 * Default is <homeDir>/.jsidplay2
	 * 
	 * @return the temporary directory for JSIDPlay2
	 */
	public String getTmpDir();

	/**
	 * Setter of the temporary directory for JSIDPlay2.
	 * 
	 * @param path
	 *            the temporary directory for JSIDPlay2
	 */
	public void setTmpDir(String path);

	/**
	 * Getter of the application window X position.
	 * 
	 * @return the application window X position
	 */
	public int getFrameX();

	/**
	 * Setter of the application window X position.
	 * 
	 * @param x
	 *            application window X position
	 */
	public void setFrameX(int x);

	/**
	 * Getter of the application window Y position.
	 * 
	 * @return the application window Y position
	 */
	public int getFrameY();

	/**
	 * Setter of the application window Y position.
	 * 
	 * @param y
	 *            application window Y position
	 */
	public void setFrameY(int y);

	/**
	 * Getter of the application window width.
	 * 
	 * @return the application window width
	 */
	public int getFrameWidth();

	/**
	 * Setter of the application window width.
	 * 
	 * @param width
	 *            application window width
	 */
	public void setFrameWidth(int width);

	/**
	 * Getter of the application window height.
	 * 
	 * @return the application window height
	 */
	public int getFrameHeight();

	/**
	 * Setter of the application window height.
	 * 
	 * @param height
	 *            application window height
	 */
	public void setFrameHeight(int height);

}