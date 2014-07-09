package sidplay.ini;

import libsidplay.common.SamplingMethod;
import sidplay.audio.Audio;
import sidplay.ini.intf.IAudioSection;

/**
 * Audio section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniAudioSection extends IniSection implements IAudioSection {
	public IniAudioSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public Audio getAudio() {
		return iniReader.getPropertyEnum("Audio", "Audio", Audio.SOUNDCARD);
	}

	@Override
	public void setAudio(Audio audio) {
		iniReader.setProperty("Audio", "Audio", audio);
	}

	/**
	 * Getter of the Playback/Recording frequency.
	 * 
	 * @return Playback/Recording frequency
	 */
	@Override
	public final int getFrequency() {
		return iniReader.getPropertyInt("Audio", "Frequency", 48000);
	}

	/**
	 * Setter of the Playback/Recording frequency.
	 * 
	 * @param freq
	 *            Playback/Recording frequency
	 */
	@Override
	public final void setFrequency(final int freq) {
		iniReader.setProperty("Audio", "Frequency", freq);
	}

	/**
	 * Getter of the sampling method.
	 * 
	 * @return the sampling method
	 */
	@Override
	public final SamplingMethod getSampling() {
		return iniReader.getPropertyEnum("Audio", "Sampling", SamplingMethod.DECIMATE);
	}

	/**
	 * Setter of the sampling method.
	 * 
	 * @param method
	 *            the sampling method
	 */
	@Override
	public final void setSampling(final SamplingMethod method) {
		iniReader.setProperty("Audio", "Sampling", method);
	}

	/**
	 * Play recorded (original) or emulated tune.
	 */
	protected boolean playOriginal;

	/**
	 * Do we play the recording?
	 * 
	 * @return play the recording
	 */
	@Override
	public final boolean isPlayOriginal() {
		return playOriginal;
	}

	/**
	 * Setter to play the recorded tune.
	 * 
	 * @param original
	 *            Play recorded (original) or emulated tune
	 */
	@Override
	public final void setPlayOriginal(final boolean original) {
		this.playOriginal = original;
	}

	/**
	 * Recorded tune filename.
	 */
	protected String mp3File;

	/**
	 * Getter of the recorded tune filename.
	 * 
	 * @return the recorded tune filename
	 */
	@Override
	public final String getMp3File() {
		return iniReader.getPropertyString("Audio", "MP3File", null);
	}

	/**
	 * Setter of the recorded tune filename.
	 * 
	 * @param recording
	 *            the recorded tune filename
	 */
	@Override
	public final void setMp3File(final String recording) {
		iniReader.setProperty("Audio", "MP3File", recording);
	}

	/**
	 * Getter of the left volume setting.
	 * 
	 * @return the left volume setting
	 */
	@Override
	public final float getLeftVolume() {
		return iniReader.getPropertyFloat("Audio", "LeftVolume", 0f);
	}

	/**
	 * Setter of the left volume setting.
	 * 
	 * @param volume
	 *            the left volume setting
	 */
	@Override
	public final void setLeftVolume(final float volume) {
		iniReader.setProperty("Audio", "LeftVolume", volume);
	}

	/**
	 * Getter of the right volume setting.
	 * 
	 * @return the right volume setting
	 */
	@Override
	public float getRightVolume() {
		return iniReader.getPropertyFloat("Audio", "RightVolume", 0f);
	}

	/**
	 * Setter of the right volume setting.
	 * 
	 * @param volume
	 *            the right volume setting
	 */
	@Override
	public void setRightVolume(final float volume) {
		iniReader.setProperty("Audio", "RightVolume", volume);
	}

}