package libsidplay.common;

/**
 * Interface for SID mixer controls. A SID builder that implements this iterface
 * gets controlled by the player.
 * 
 * @author ken
 *
 */
public interface Mixer {

	/**
	 * Volume of the SID chip
	 * 
	 * @param sidNum
	 *            SID chip number
	 */
	void setVolume(int sidNum);

	/**
	 * Fade-in start time reached, audio volume should be increased to the max.
	 * 
	 * @param fadeIn
	 *            Fade-in time in seconds
	 */
	void fadeIn(int fadeIn);

	/**
	 * Fade-out start time reached, audio volume should be lowered to zero.
	 * 
	 * @param fadeOut
	 *            Fade-out time in seconds
	 */
	void fadeOut(int fadeOut);

	/**
	 * Panning feature: spreading of the SID chip sound signal to the two stereo
	 * channels
	 * 
	 * @param sidNum
	 *            SID chip number
	 */
	void setBalance(int sidNum);

	/**
	 * Doubles speed factor.
	 */
	void fastForward();

	/**
	 * Use normal speed factor.
	 */
	void normalSpeed();

	/**
	 * @return speed factor is used?
	 */
	boolean isFastForward();

}
