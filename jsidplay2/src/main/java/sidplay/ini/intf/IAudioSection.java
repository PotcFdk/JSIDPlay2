package sidplay.ini.intf;

import resid_builder.resid.SamplingMethod;

public interface IAudioSection {

	/**
	 * Getter of the Playback/Recording frequency.
	 * 
	 * @return Playback/Recording frequency
	 */
	public int getFrequency();

	/**
	 * Setter of the Playback/Recording frequency.
	 * 
	 * @param freq
	 *            Playback/Recording frequency
	 */
	public void setFrequency(int freq);

	/**
	 * Getter of the sampling method.
	 * 
	 * @return the sampling method
	 */
	public SamplingMethod getSampling();

	/**
	 * Setter of the sampling method.
	 * 
	 * @param method
	 *            the sampling method
	 */
	public void setSampling(SamplingMethod method);

	/**
	 * Do we play the recording?
	 * 
	 * @return play the recording
	 */
	public boolean isPlayOriginal();

	/**
	 * Setter to play the recorded tune.
	 * 
	 * @param original
	 *            Play recorded (original) or emulated tune
	 */
	public void setPlayOriginal(boolean original);

	/**
	 * Getter of the recorded tune filename.
	 * 
	 * @return the recorded tune filename
	 */
	public String getMp3File();

	/**
	 * Setter of the recorded tune filename.
	 * 
	 * @param recording
	 *            the recorded tune filename
	 */
	public void setMp3File(String recording);

	/**
	 * Getter of the left volume setting.
	 * 
	 * @return the left volume setting
	 */
	public float getLeftVolume();

	/**
	 * Setter of the left volume setting.
	 * 
	 * @param volume
	 *            the left volume setting
	 */
	public void setLeftVolume(float volume);

	/**
	 * Getter of the right volume setting.
	 * 
	 * @return the right volume setting
	 */
	public float getRightVolume();

	/**
	 * Setter of the right volume setting.
	 * 
	 * @param volume
	 *            the right volume setting
	 */
	public void setRightVolume(float volume);

}