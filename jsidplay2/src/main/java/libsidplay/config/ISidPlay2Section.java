package libsidplay.config;

public interface ISidPlay2Section {

	static final boolean DEFAULT_ENABLE_DATABASE = true;
	static final int DEFAULT_PLAY_LENGTH = 3 * 60;
	static final boolean DEFAULT_LOOP = false;
	static final boolean DEFAULT_SINGLE_TRACK = false;
	static final boolean DEFAULT_ENABLE_PROXY = false;
	static final boolean DEFAULT_TURBO_TAPE = true;
	
	/**
	 * Get INI file version.
	 * 
	 * @return INI file version
	 */
	int getVersion();

	/**
	 * Set configuration version
	 * 
	 * @param version
	 *            configuration version
	 */
	void setVersion(int version);

	/**
	 * Getter of the enable of the Songlengths database.
	 * 
	 * @return Is the Songlengths database enabled?
	 */
	boolean isEnableDatabase();

	/**
	 * Setter of the enable of the Songlengths database.
	 * 
	 * @param enable
	 *            the enable of the Songlengths database
	 */
	void setEnableDatabase(boolean enable);

	/**
	 * Getter of the default play length (if the song length is unknown).
	 * 
	 * @return default play length
	 */
	int getDefaultPlayLength();

	/**
	 * Setter of the default play length (if the song length is unknown).
	 * 
	 * @param playLength
	 *            default play length
	 */
	void setDefaultPlayLength(int playLength);

	boolean isLoop();

	void setLoop(boolean loop);

	/**
	 * Getter of the HVSC collection directory.
	 * 
	 * @return the HVSC collection directory
	 */
	String getHvsc();

	/**
	 * Setter of the HVSC collection directory.
	 * 
	 * @param hvsc
	 *            the HVSC collection directory
	 */
	void setHvsc(String hvsc);

	/**
	 * Do we play a single song per tune?
	 * 
	 * @return play a single song per tune
	 */
	boolean isSingle();

	/**
	 * setter to play a single song per tune.
	 * 
	 * @param singleSong
	 *            play a single song per tune
	 */
	void setSingle(boolean singleSong);

	/**
	 * Getter of the last accessed directory in the file browser.
	 * 
	 * @return the last accessed directory in the file browser
	 */
	String getLastDirectory();

	/**
	 * Setter of the last accessed directory in the file browser.
	 * 
	 * @param lastDir
	 *            the last accessed directory in the file browser
	 */
	void setLastDirectory(String lastDir);

	/**
	 * Getter of the temporary directory for JSIDPlay2.
	 * 
	 * Default is <homeDir>/.jsidplay2
	 * 
	 * @return the temporary directory for JSIDPlay2
	 */
	String getTmpDir();

	/**
	 * Setter of the temporary directory for JSIDPlay2.
	 * 
	 * @param path
	 *            the temporary directory for JSIDPlay2
	 */
	void setTmpDir(String path);

	/**
	 * Getter of convert other programs to turbo-tape or normal tape format
	 * 
	 * @return should be converted to turbo tape
	 */
	boolean isTurboTape();

	/**
	 * Setter of convert other programs to turbo-tape or normal tape format
	 */
	void setTurboTape(boolean turboTape);
}