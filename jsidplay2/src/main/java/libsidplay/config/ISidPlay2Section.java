package libsidplay.config;

public interface ISidPlay2Section {

	static final boolean DEFAULT_ENABLE_DATABASE = true;
	static final int DEFAULT_PLAY_LENGTH = 3 * 60;
	static final int DEFAULT_FADE_IN_TIME = 0;
	static final int DEFAULT_FADE_OUT_TIME = 0;
	static final boolean DEFAULT_LOOP = false;
	static final boolean DEFAULT_SINGLE_TRACK = false;
	static final boolean DEFAULT_ENABLE_PROXY = false;
	static final float DEFAULT_BRIGHTNESS = 0f;
	static final float DEFAULT_CONTRAST = 1f;
	static final float DEFAULT_GAMMA = 2f;
	static final float DEFAULT_SATURATION = .5f;
	static final float DEFAULT_PHASE_SHIFT = -15f;
	static final float DEFAULT_OFFSET = .9f;
	static final float DEFAULT_TINT = 0f;
	static final float DEFAULT_BLUR = .5f;
	static final float DEFAULT_BLEED = .5f;
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
	 * Get VIC palette setting brightness.
	 * 
	 * @return VIC palette setting brightness
	 */
	public float getBrightness();

	/**
	 * Set VIC palette setting brightness
	 * 
	 * @param brightness
	 *            brightness
	 */
	public void setBrightness(float brightness);

	/**
	 * Get VIC palette setting contrast.
	 * 
	 * @return VIC palette setting contrast
	 */
	public float getContrast();

	/**
	 * Set VIC palette setting contrast
	 * 
	 * @param contrast
	 *            contrast
	 */
	public void setContrast(float contrast);

	/**
	 * Get VIC palette setting gamma.
	 * 
	 * @return VIC palette setting gamma
	 */
	public float getGamma();

	/**
	 * Set VIC palette setting gamma
	 * 
	 * @param gamma
	 *            gamma
	 */
	public void setGamma(float gamma);

	/**
	 * Get VIC palette setting saturation.
	 * 
	 * @return VIC palette setting saturation
	 */
	public float getSaturation();

	/**
	 * Set VIC palette setting saturation
	 * 
	 * @param saturation
	 *            saturation
	 */
	public void setSaturation(float saturation);

	/**
	 * Get VIC palette setting phaseShift.
	 * 
	 * @return VIC palette setting phaseShift
	 */
	public float getPhaseShift();

	/**
	 * Set VIC palette setting phaseShift
	 * 
	 * @param phaseShift
	 *            phaseShift
	 */
	public void setPhaseShift(float phaseShift);

	/**
	 * Get VIC palette setting offset.
	 * 
	 * @return VIC palette setting offset
	 */
	public float getOffset();

	/**
	 * Set VIC palette setting offset
	 * 
	 * @param offset
	 *            offset
	 */
	public void setOffset(float offset);

	/**
	 * Get VIC palette setting tint.
	 * 
	 * @return VIC palette setting tint
	 */
	public float getTint();

	/**
	 * Set VIC palette setting tint
	 * 
	 * @param tint
	 *            tint
	 */
	public void setTint(float tint);

	/**
	 * Get VIC palette setting blur.
	 * 
	 * @return VIC palette setting blur
	 */
	public float getBlur();

	/**
	 * Set VIC palette setting blur
	 * 
	 * @param blur
	 *            blur
	 */
	public void setBlur(float blur);

	/**
	 * Get VIC palette setting bleed.
	 * 
	 * @return VIC palette setting bleed
	 */
	public float getBleed();

	/**
	 * Set VIC palette setting bleed
	 * 
	 * @param bleed
	 *            bleed
	 */
	public void setBleed(float bleed);

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

	/**
	 * Fade-in start time in seconds, audio volume should be increased to the
	 * max.
	 */
	int getFadeInTime();

	/**
	 * Fade-out start time in seconds, audio volume should be lowered to zero.
	 */
	int getFadeOutTime();
}