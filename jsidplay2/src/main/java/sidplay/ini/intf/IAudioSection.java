package sidplay.ini.intf;

import libsidplay.common.SamplingMethod;
import sidplay.audio.Audio;

public interface IAudioSection {

	/**
	 * Getter of the audio to be used.
	 * 
	 * @return the audio to be used
	 */
	Audio getAudio();

	/**
	 * Setter of the audio to be used.
	 * 
	 * @param emulation
	 *            audio to be used
	 */
	void setAudio(Audio audio);

	/**
	 * Getter of the SID driver to play SIDs.
	 * 
	 * @return SID driver to play SIDs
	 */
	String getSidDriver();
	
	/**
	 * Setter of the SID driver to play SIDs.
	 * 
	 * @param sidDriver SID driver to play SIDs
	 */
	void setSidDriver(final String sidDriver);

	int getDevice();

	void setDevice(int device);

	/**
	 * Getter of the Playback/Recording frequency.
	 * 
	 * @return Playback/Recording frequency
	 */
	int getFrequency();

	/**
	 * Setter of the Playback/Recording frequency.
	 * 
	 * @param freq
	 *            Playback/Recording frequency
	 */
	void setFrequency(int freq);

	/**
	 * Getter of the sampling method.
	 * 
	 * @return the sampling method
	 */
	SamplingMethod getSampling();

	/**
	 * Setter of the sampling method.
	 * 
	 * @param method
	 *            the sampling method
	 */
	void setSampling(SamplingMethod method);

	/**
	 * Do we play the recording?
	 * 
	 * @return play the recording
	 */
	boolean isPlayOriginal();

	/**
	 * Setter to play the recorded tune.
	 * 
	 * @param original
	 *            Play recorded (original) or emulated tune
	 */
	void setPlayOriginal(boolean original);

	/**
	 * Getter of the recorded tune filename.
	 * 
	 * @return the recorded tune filename
	 */
	String getMp3File();

	/**
	 * Setter of the recorded tune filename.
	 * 
	 * @param recording
	 *            the recorded tune filename
	 */
	void setMp3File(String recording);

	/**
	 * Getter of the left volume setting.
	 * 
	 * @return the left volume setting
	 */
	float getLeftVolume();

	/**
	 * Setter of the left volume setting.
	 * 
	 * @param volume
	 *            the left volume setting
	 */
	void setLeftVolume(float volume);

	/**
	 * Getter of the right volume setting.
	 * 
	 * @return the right volume setting
	 */
	float getRightVolume();

	/**
	 * Setter of the right volume setting.
	 * 
	 * @param volume
	 *            the right volume setting
	 */
	void setRightVolume(float volume);

}