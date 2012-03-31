package sidplay.ini;

import java.io.File;

import resid_builder.resid.ISIDDefs.SamplingMethod;
import sidplay.audio.AudioConfig;

/**
 * Audio section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniAudioSection extends IniSection {
	public IniAudioSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Getter of the Playback/Recording frequency.
	 * 
	 * @return Playback/Recording frequency
	 */
	public final int getFrequency() {
		return iniReader.getPropertyInt("Audio", "Frequency", 48000);
	}

	/**
	 * Setter of the Playback/Recording frequency.
	 * 
	 * @param freq
	 *            Playback/Recording frequency
	 */
	public final void setFrequency(final int freq) {
		iniReader.setProperty("Audio", "Frequency", freq);
	}

	/**
	 * Getter of the sampling method.
	 * 
	 * @return the sampling method
	 */
	public final SamplingMethod getSampling() {
		return iniReader.getPropertyEnum("Audio", "Sampling", SamplingMethod.DECIMATE);
	}

	/**
	 * Setter of the sampling method.
	 * 
	 * @param method
	 *            the sampling method
	 */
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
	public final boolean isPlayOriginal() {
		return playOriginal;
	}

	/**
	 * Setter to play the recorded tune.
	 * 
	 * @param original
	 *            Play recorded (original) or emulated tune
	 */
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
	public final File getMp3File() {
		return new File(iniReader.getPropertyString("Audio", "MP3File", null));
	}

	/**
	 * Setter of the recorded tune filename.
	 * 
	 * @param recording
	 *            the recorded tune filename
	 */
	public final void setMp3File(final File recording) {
		iniReader.setProperty("Audio", "MP3File", recording.getAbsolutePath());
	}

	/**
	 * Getter of the left volume setting.
	 * 
	 * @return the left volume setting
	 */
	public final float getLeftVolume() {
		return iniReader.getPropertyFloat("Audio", "LeftVolume", 0f);
	}

	/**
	 * Setter of the left volume setting.
	 * 
	 * @param volume
	 *            the left volume setting
	 */
	public final void setLeftVolume(final float volume) {
		iniReader.setProperty("Audio", "LeftVolume", volume);
	}

	/**
	 * Getter of the right volume setting.
	 * 
	 * @return the right volume setting
	 */
	public float getRightVolume() {
		return iniReader.getPropertyFloat("Audio", "RightVolume", 0f);
	}

	/**
	 * Setter of the right volume setting.
	 * 
	 * @param volume
	 *            the right volume setting
	 */
	public void setRightVolume(final float volume) {
		iniReader.setProperty("Audio", "RightVolume", volume);
	}

	/**
	 * Return a detached AudioConfig instance corresponding to current parameters.
	 * 
	 * @param channels
	 * @return AudioConfig for current specification
	 */
	public AudioConfig toAudioConfig(int channels) {
		return new AudioConfig(getFrequency(), channels, getSampling());
	}
}