package libsidplay.config;

public interface ISidPlay2Section {

	/**
	 * Get INI file version.
	 *
	 * @return INI file version
	 */
	int getVersion();

	/**
	 * Set configuration version
	 *
	 * @param version configuration version
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
	 * @param enable the enable of the Songlengths database
	 */
	void setEnableDatabase(boolean enable);

	/**
	 * Getter of the start time.
	 *
	 * @return start time
	 */
	double getStartTime();

	/**
	 * Setter of the start time.
	 *
	 * @param startTime start time
	 */
	void setStartTime(double startTime);

	/**
	 * Getter of the default play length (if the song length is unknown).
	 *
	 * @return default play length
	 */
	double getDefaultPlayLength();

	/**
	 * Setter of the default play length (if the song length is unknown).
	 *
	 * @param playLength default play length
	 */
	void setDefaultPlayLength(double playLength);

	/**
	 * Getter of the tune should loop
	 * 
	 * @return tune should loop
	 */
	boolean isLoop();

	/**
	 * Setter of the tune should loop
	 * 
	 * @param loop tune should loop
	 */
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
	 * @param hvsc the HVSC collection directory
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
	 * @param singleSong play a single song per tune
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
	 * @param lastDir the last accessed directory in the file browser
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
	 * @param path the temporary directory for JSIDPlay2
	 */
	void setTmpDir(String path);

	/**
	 * Get PAL emulation enable.
	 *
	 * @return PAL emulation enable
	 */
	boolean isPalEmulation();

	/**
	 * Set PAL emulation enable.
	 *
	 * @param palEmulation PAL emulation enable
	 */
	void setPalEmulation(boolean palEmulation);

	/**
	 * Get VIC palette setting brightness.
	 *
	 * @return VIC palette setting brightness
	 */
	float getBrightness();

	/**
	 * Set VIC palette setting brightness
	 *
	 * @param brightness brightness
	 */
	void setBrightness(float brightness);

	/**
	 * Get VIC palette setting contrast.
	 *
	 * @return VIC palette setting contrast
	 */
	float getContrast();

	/**
	 * Set VIC palette setting contrast
	 *
	 * @param contrast contrast
	 */
	void setContrast(float contrast);

	/**
	 * Get VIC palette setting gamma.
	 *
	 * @return VIC palette setting gamma
	 */
	float getGamma();

	/**
	 * Set VIC palette setting gamma
	 *
	 * @param gamma gamma
	 */
	void setGamma(float gamma);

	/**
	 * Get VIC palette setting saturation.
	 *
	 * @return VIC palette setting saturation
	 */
	float getSaturation();

	/**
	 * Set VIC palette setting saturation
	 *
	 * @param saturation saturation
	 */
	void setSaturation(float saturation);

	/**
	 * Get VIC palette setting phaseShift.
	 *
	 * @return VIC palette setting phaseShift
	 */
	float getPhaseShift();

	/**
	 * Set VIC palette setting phaseShift
	 *
	 * @param phaseShift phaseShift
	 */
	void setPhaseShift(float phaseShift);

	/**
	 * Get VIC palette setting offset.
	 *
	 * @return VIC palette setting offset
	 */
	float getOffset();

	/**
	 * Set VIC palette setting offset
	 *
	 * @param offset offset
	 */
	void setOffset(float offset);

	/**
	 * Get VIC palette setting tint.
	 *
	 * @return VIC palette setting tint
	 */
	float getTint();

	/**
	 * Set VIC palette setting tint
	 *
	 * @param tint tint
	 */
	void setTint(float tint);

	/**
	 * Get VIC palette setting blur.
	 *
	 * @return VIC palette setting blur
	 */
	float getBlur();

	/**
	 * Set VIC palette setting blur
	 *
	 * @param blur blur
	 */
	void setBlur(float blur);

	/**
	 * Get VIC palette setting bleed.
	 *
	 * @return VIC palette setting bleed
	 */
	float getBleed();

	/**
	 * Set VIC palette setting bleed
	 *
	 * @param bleed bleed
	 */
	void setBleed(float bleed);

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
	 * Fade-in start time in seconds, audio volume should be increased to the max.
	 */
	double getFadeInTime();

	/**
	 * Setter of Fade-in start time in seconds, audio volume should be increased to
	 * the max.
	 */
	void setFadeInTime(double fadeInTime);

	/**
	 * Fade-out start time in seconds, audio volume should be lowered to zero.
	 */
	double getFadeOutTime();

	/**
	 * Setter of Fade-out start time in seconds, audio volume should be lowered to
	 * zero.
	 */
	void setFadeOutTime(double fadeOutTime);

}