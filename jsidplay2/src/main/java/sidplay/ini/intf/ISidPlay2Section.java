package sidplay.ini.intf;

public interface ISidPlay2Section {

	public static final boolean DEFAULT_ENABLE_DATABASE = true;
	public static final int DEFAULT_PLAY_LENGTH = 3 * 60;
	public static final boolean DEFAULT_LOOP = false;
	public static final boolean DEFAULT_SINGLE_TRACK = false;
	public static final boolean DEFAULT_ENABLE_PROXY = false;

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

}