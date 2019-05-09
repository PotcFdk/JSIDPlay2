package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_MP3_FILE;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_ORIGINAL;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_VOLUME;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;

/**
 * Audio section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
@Parameters(resourceBundle = "sidplay.ini.IniAudioSection")
public class IniAudioSection extends IniSection implements IAudioSection {
	public IniAudioSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public Audio getAudio() {
		return iniReader.getPropertyEnum("Audio", "Audio", DEFAULT_AUDIO, Audio.class);
	}

	@Override
	@Parameter(names = { "--audio", "-a" }, descriptionKey = "DRIVER")
	public void setAudio(Audio audio) {
		iniReader.setProperty("Audio", "Audio", audio);
	}

	@Override
	public int getDevice() {
		return iniReader.getPropertyInt("Audio", "Device", DEFAULT_DEVICE);
	}

	@Override
	@Parameter(names = { "--deviceIndex", "-A" }, descriptionKey = "DEVICEINDEX")
	public void setDevice(int device) {
		iniReader.setProperty("Audio", "Device", device);
	}

	/**
	 * Getter of the Playback/Recording frequency.
	 * 
	 * @return Playback/Recording frequency
	 */
	@Override
	public final SamplingRate getSamplingRate() {
		return iniReader.getPropertyEnum("Audio", "Sampling Rate", DEFAULT_SAMPLING_RATE, SamplingRate.class);
	}

	/**
	 * Setter of the Playback/Recording frequency.
	 * 
	 * @param samplingRate Playback/Recording frequency
	 */
	@Override
	@Parameter(names = { "--frequency", "-f" }, descriptionKey = "FREQUENCY")
	public final void setSamplingRate(final SamplingRate samplingRate) {
		iniReader.setProperty("Audio", "Sampling Rate", samplingRate);
	}

	/**
	 * Getter of the sampling method.
	 * 
	 * @return the sampling method
	 */
	@Override
	public final SamplingMethod getSampling() {
		return iniReader.getPropertyEnum("Audio", "Sampling", DEFAULT_SAMPLING, SamplingMethod.class);
	}

	/**
	 * Setter of the sampling method.
	 * 
	 * @param method the sampling method
	 */
	@Override
	@Parameter(names = { "--sampling" }, descriptionKey = "SAMPLING")
	public final void setSampling(final SamplingMethod method) {
		iniReader.setProperty("Audio", "Sampling", method);
	}

	/**
	 * Do we play the recording?
	 * 
	 * @return play the recording
	 */
	@Override
	public final boolean isPlayOriginal() {
		return iniReader.getPropertyBool("Audio", "Play Original", DEFAULT_PLAY_ORIGINAL);
	}

	/**
	 * Setter to play the recorded tune.
	 * 
	 * @param original Play recorded (original) or emulated tune
	 */
	@Override
	public final void setPlayOriginal(final boolean original) {
		iniReader.setProperty("Audio", "Play Original", original);
	}

	/**
	 * Getter of the recorded tune filename.
	 * 
	 * @return the recorded tune filename
	 */
	@Override
	public final String getMp3File() {
		return iniReader.getPropertyString("Audio", "MP3File", DEFAULT_MP3_FILE);
	}

	/**
	 * Setter of the recorded tune filename.
	 * 
	 * @param recording the recorded tune filename
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
		return iniReader.getPropertyFloat("Audio", "MainVolume", DEFAULT_MAIN_VOLUME);
	}

	/**
	 * Setter of the main SID volume setting.
	 * 
	 * @param volume the main SID volume setting
	 */
	@Override
	@Parameter(names = { "--mainVolume" }, descriptionKey = "MAIN_VOLUME")
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
		return iniReader.getPropertyFloat("Audio", "SecondVolume", DEFAULT_SECOND_VOLUME);
	}

	/**
	 * Setter of the second SID volume setting.
	 * 
	 * @param volume the second SID volume setting
	 */
	@Override
	@Parameter(names = { "--secondVolume" }, descriptionKey = "SECOND_VOLUME")
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
		return iniReader.getPropertyFloat("Audio", "ThirdVolume", DEFAULT_THIRD_VOLUME);
	}

	/**
	 * Setter of the third SID volume setting.
	 * 
	 * @param volume the third SID volume setting
	 */
	@Override
	@Parameter(names = { "--thirdVolume" }, descriptionKey = "THIRD_VOLUME")
	public void setThirdVolume(final float volume) {
		iniReader.setProperty("Audio", "ThirdVolume", volume);
	}

	@Override
	public float getMainBalance() {
		return iniReader.getPropertyFloat("Audio", "MainBalance", DEFAULT_MAIN_BALANCE);
	}

	@Override
	@Parameter(names = { "--mainBalance" }, descriptionKey = "MAIN_BALANCE")
	public void setMainBalance(float balance) {
		iniReader.setProperty("Audio", "MainBalance", balance);
	}

	@Override
	public float getSecondBalance() {
		return iniReader.getPropertyFloat("Audio", "SecondBalance", DEFAULT_SECOND_BALANCE);
	}

	@Override
	@Parameter(names = { "--secondBalance" }, descriptionKey = "SECOND_BALANCE")
	public void setSecondBalance(float balance) {
		iniReader.setProperty("Audio", "SecondBalance", balance);
	}

	@Override
	public float getThirdBalance() {
		return iniReader.getPropertyFloat("Audio", "ThirdBalance", DEFAULT_THIRD_BALANCE);
	}

	@Override
	@Parameter(names = { "--thirdBalance" }, descriptionKey = "THIRD_BALANCE")
	public void setThirdBalance(float balance) {
		iniReader.setProperty("Audio", "ThirdBalance", balance);
	}

	@Override
	public int getMainDelay() {
		return iniReader.getPropertyInt("Audio", "MainDelay", DEFAULT_MAIN_DELAY);
	}

	@Override
	@Parameter(names = { "--mainDelay" }, descriptionKey = "MAIN_DELAY")
	public void setMainDelay(int delay) {
		iniReader.setProperty("Audio", "MainDelay", delay);
	}

	@Override
	public int getSecondDelay() {
		return iniReader.getPropertyInt("Audio", "SecondDelay", DEFAULT_SECOND_DELAY);
	}

	@Override
	@Parameter(names = { "--secondDelay" }, descriptionKey = "SECOND_DELAY")
	public void setSecondDelay(int delay) {
		iniReader.setProperty("Audio", "SecondDelay", delay);
	}

	@Override
	public int getThirdDelay() {
		return iniReader.getPropertyInt("Audio", "ThirdDelay", DEFAULT_THIRD_DELAY);
	}

	@Override
	@Parameter(names = { "--thirdDelay" }, descriptionKey = "THIRD_DELAY")
	public void setThirdDelay(int delay) {
		iniReader.setProperty("Audio", "ThirdDelay", delay);
	}

	@Override
	public int getBufferSize() {
		return iniReader.getPropertyInt("Audio", "Buffer Size", DEFAULT_BUFFER_SIZE);
	}

	@Override
	@Parameter(names = { "--bufferSize", "-B" }, descriptionKey = "BUFFER_SIZE")
	public void setBufferSize(int bufferSize) {
		iniReader.setProperty("Audio", "Buffer Size", bufferSize);
	}

	@Override
	public int getAudioBufferSize() {
		return iniReader.getPropertyInt("Audio", "Audio Buffer Size", DEFAULT_AUDIO_BUFFER_SIZE);
	}

	@Override
	@Parameter(names = { "--audioBufferSize" }, descriptionKey = "AUDIO_BUFFER_SIZE")
	public void setAudioBufferSize(int audioBufferSize) {
		iniReader.setProperty("Audio", "Audio Buffer Size", audioBufferSize);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("audio=").append(getAudio()).append(",");
		result.append("device=").append(getDevice()).append(",");
		result.append("samplingRate=").append(getSamplingRate()).append(",");
		result.append("sampling=").append(getSampling()).append(",");
		result.append("playOriginal=").append(isPlayOriginal()).append(",");
		result.append("mp3File=").append(getMp3File()).append(",");
		result.append("mainVolume=").append(getMainVolume()).append(",");
		result.append("secondVolume=").append(getSecondVolume()).append(",");
		result.append("thirdVolume=").append(getThirdVolume()).append(",");
		result.append("mainBalance=").append(getMainBalance()).append(",");
		result.append("secondBalance=").append(getSecondBalance()).append(",");
		result.append("thirdBalance=").append(getThirdBalance()).append(",");
		result.append("mainDelay=").append(getMainDelay()).append(",");
		result.append("secondDelay=").append(getSecondDelay()).append(",");
		result.append("thirdDelay=").append(getThirdDelay()).append(",");
		result.append("bufferSize=").append(getBufferSize());
		return result.toString();
	}

}