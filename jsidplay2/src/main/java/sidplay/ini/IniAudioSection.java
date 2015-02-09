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

	protected String sidDriver;

	@Override
	public String getSidDriver() {
		return iniReader.getPropertyString("Audio", "SIDDriver",
				"/libsidplay/sidtune/psiddriver.asm");
	}

	@Override
	public void setSidDriver(String sidDriver) {
		iniReader.setProperty("Audio", "SIDDriver", sidDriver);
	}

	@Override
	public int getDevice() {
		return iniReader.getPropertyInt("Audio", "Device", 0);
	}

	@Override
	public void setDevice(int device) {
		iniReader.setProperty("Audio", "Device", device);
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
		return iniReader.getPropertyEnum("Audio", "Sampling",
				SamplingMethod.DECIMATE);
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
	 * Getter of the main SID volume setting.
	 * 
	 * @return the main SID volume setting
	 */
	@Override
	public final float getMainVolume() {
		return iniReader.getPropertyFloat("Audio", "MainVolume", 0f);
	}

	/**
	 * Setter of the main SID volume setting.
	 * 
	 * @param volume
	 *            the main SID volume setting
	 */
	@Override
	public final void setMainVolume(final float volume) {
		iniReader.setProperty("Audio", "MainVolume", volume);
	}

	/**
	 * Getter of the second SID volume setting.
	 * 
	 * @return the second SID volume setting
	 */
	@Override
	public float getSecondVolume() {
		return iniReader.getPropertyFloat("Audio", "SecondVolume", 0f);
	}

	/**
	 * Setter of the second SID volume setting.
	 * 
	 * @param volume
	 *            the second SID volume setting
	 */
	@Override
	public void setSecondVolume(final float volume) {
		iniReader.setProperty("Audio", "SecondVolume", volume);
	}

	/**
	 * Getter of the third SID volume setting.
	 * 
	 * @return the third SID volume setting
	 */
	@Override
	public float getThirdVolume() {
		return iniReader.getPropertyFloat("Audio", "ThirdVolume", 0f);
	}

	/**
	 * Setter of the third SID volume setting.
	 * 
	 * @param volume
	 *            the third SID volume setting
	 */
	@Override
	public void setThirdVolume(final float volume) {
		iniReader.setProperty("Audio", "ThirdVolume", volume);
	}

	@Override
	public float getMainBalance() {
		return iniReader.getPropertyFloat("Audio", "MainBalance", 0f);
	}

	@Override
	public void setMainBalance(float balance) {
		iniReader.setProperty("Audio", "MainBalance", balance);
	}

	@Override
	public float getSecondBalance() {
		return iniReader.getPropertyFloat("Audio", "SecondBalance", 1f);
	}

	@Override
	public void setSecondBalance(float balance) {
		iniReader.setProperty("Audio", "SecondBalance", balance);
	}

	@Override
	public float getThirdBalance() {
		return iniReader.getPropertyFloat("Audio", "ThirdBalance", .5f);
	}

	@Override
	public void setThirdBalance(float balance) {
		iniReader.setProperty("Audio", "ThirdBalance", balance);
	}

	/*
	 * supports 5 ms chunk at 96 kHz
	 */
	private int bufferSize = 5000;

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
}