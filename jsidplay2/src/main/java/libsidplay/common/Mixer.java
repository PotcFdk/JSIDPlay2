package libsidplay.common;

import sidplay.audio.AudioDriver;

/**
 * Interface for SID mixer controls. A SID builder that implements this
 * interface gets controlled by the player.
 * 
 * @author ken
 *
 */
public interface Mixer {

	/**
	 * Maximum fast forward factor (1 << 5 = 32x).
	 */
	public static final int MAX_FAST_FORWARD = 5;

	/**
	 * Set audio driver for mixing
	 */
	void setAudioDriver(AudioDriver audioDriver);
	
	/**
	 * Timer start reached, audio output should be produced.
	 */
	void start();

	/**
	 * Fade-in start time reached, audio volume should be increased to the max.
	 * 
	 * @param fadeIn
	 *            Fade-in time in seconds
	 */
	void fadeIn(double fadeIn);

	/**
	 * Fade-out start time reached, audio volume should be lowered to zero.
	 * 
	 * @param fadeOut
	 *            Fade-out time in seconds
	 */
	void fadeOut(double fadeOut);

	/**
	 * Volume of the SID chip
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param volume
	 *            volume in DB -6(-6db)..6(+6db)
	 */
	void setVolume(int sidNum, float volume);

	/**
	 * Panning feature: spreading of the SID chip sound signal to the two stereo
	 * channels
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param balance
	 *            balance 0(left speaker)..0.5(centered)..1(right speaker)
	 */
	void setBalance(int sidNum, float balance);

	/**
	 * Delay feature: Delaying SID chip sound samples by time in milliseconds
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param delay
	 *            delay in ms
	 */
	void setDelay(int sidNum, int delay);

	/**
	 * Pause player.
	 */
	default void pause() {
	}

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

	/**
	 * @return bit mask of current speed factor (1 << 0 = 1x, 1 << 1 = 2x, ... , 1 << 5 = 32x) - 1
	 */
	int getFastForwardBitMask();

}
