package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_DRY_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_FEEDBACK_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_WET_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_DISTORTION_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DISTORTION_GAIN;
import static sidplay.ini.IniDefaults.DEFAULT_DISTORTION_THRESHOLD;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_MP3_FILE;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_ORIGINAL;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB1_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB2_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB3_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB4_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_DRY_WET_MIX;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_SUSTAIN_DELAY;
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
	public boolean getDelayBypass() {
		return iniReader.getPropertyBool("Audio", "Delay Bypass", DEFAULT_DELAY_BYPASS);
	}

	@Override
	@Parameter(names = { "--delayBypass" }, descriptionKey = "DELAY_BYPASS", arity=1)
	public void setDelayBypass(boolean delayBypass) {
		iniReader.setProperty("Audio", "Delay Bypass", delayBypass);
	}

	@Override
	public int getDelay() {
		return iniReader.getPropertyInt("Audio", "Delay", DEFAULT_DELAY);
	}

	@Override
	@Parameter(names = { "--delay" }, descriptionKey = "DELAY")
	public void setDelay(int delay) {
		iniReader.setProperty("Audio", "Delay", delay);
	}

	@Override
	public int getDelayWetLevel() {
		return iniReader.getPropertyInt("Audio", "Delay Wet Level", DEFAULT_DELAY_WET_LEVEL);
	}

	@Override
	@Parameter(names = { "--delayWetLevel" }, descriptionKey = "DELAY_WET_LEVEL")
	public void setDelayWetLevel(int delayWetLevel) {
		iniReader.setProperty("Audio", "Delay Wet Level", delayWetLevel);
	}

	@Override
	public int getDelayDryLevel() {
		return iniReader.getPropertyInt("Audio", "Delay Dry Level", DEFAULT_DELAY_DRY_LEVEL);
	}

	@Override
	@Parameter(names = { "--delayDryLevel" }, descriptionKey = "DELAY_DRY_LEVEL")
	public void setDelayDryLevel(int delayDryLevel) {
		iniReader.setProperty("Audio", "Delay Dry Level", delayDryLevel);
	}

	@Override
	public int getDelayFeedbackLevel() {
		return iniReader.getPropertyInt("Audio", "Delay Feedback Level", DEFAULT_DELAY_FEEDBACK_LEVEL);
	}

	@Override
	@Parameter(names = { "--delayFeedbackLevel" }, descriptionKey = "DELAY_FEEDBACK_LEVEL")
	public void setDelayFeedbackLevel(int delayFeedbackLevel) {
		iniReader.setProperty("Audio", "Delay Feedback Level", delayFeedbackLevel);
	}

	@Override
	public boolean getDistortionBypass() {
		return iniReader.getPropertyBool("Audio", "Distortion Bypass", DEFAULT_DISTORTION_BYPASS);
	}

	@Override
	@Parameter(names = { "--distortionBypass" }, descriptionKey = "DISTORTION_BYPASS", arity=1)
	public void setDistortionBypass(boolean distortionBypass) {
		iniReader.setProperty("Audio", "Distortion Bypass", distortionBypass);
	}

	@Override
	public int getDistortionThreshold() {
		return iniReader.getPropertyInt("Audio", "Distortion Threshold", DEFAULT_DISTORTION_THRESHOLD);
	}

	@Override
	@Parameter(names = { "--distortionThreshold" }, descriptionKey = "DISTORTION_THRESHOLD")
	public void setDistortionThreshold(int distortionThreshold) {
		iniReader.setProperty("Audio", "Distortion Threshold", distortionThreshold);
	}

	@Override
	public float getDistortionGain() {
		return iniReader.getPropertyFloat("Audio", "Distortion Gain", DEFAULT_DISTORTION_GAIN);
	}

	@Override
	@Parameter(names = { "--distortionGain" }, descriptionKey = "DISTORTION_GAIN")
	public void setDistortionGain(float distortionGain) {
		iniReader.setProperty("Audio", "Distortion Gain", distortionGain);
	}

	@Override
	public boolean getReverbBypass() {
		return iniReader.getPropertyBool("Audio", "Reverb Bypass", DEFAULT_REVERB_BYPASS);
	}

	@Override
	@Parameter(names = { "--reverbBypass" }, descriptionKey = "REVERB_BYPASS", arity=1)
	public void setReverbBypass(boolean reverbBypass) {
		iniReader.setProperty("Audio", "Reverb Bypass", reverbBypass);
	}

	@Override
	public float getReverbComb1Delay() {
		return iniReader.getPropertyFloat("Audio", "Reverb Comb1 Delay", DEFAULT_REVERB_COMB1_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb1Delay" }, descriptionKey = "REVERB_COMB1_DELAY")
	public void setReverbComb1Delay(float reverbComb1Delay) {
		iniReader.setProperty("Audio", "Reverb Comb1 Delay", reverbComb1Delay);
	}

	@Override
	public float getReverbComb2Delay() {
		return iniReader.getPropertyFloat("Audio", "Reverb Comb2 Delay", DEFAULT_REVERB_COMB2_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb2Delay" }, descriptionKey = "REVERB_COMB2_DELAY")
	public void setReverbComb2Delay(float reverbComb2Delay) {
		iniReader.setProperty("Audio", "Reverb Comb2 Delay", reverbComb2Delay);
	}

	@Override
	public float getReverbComb3Delay() {
		return iniReader.getPropertyFloat("Audio", "Reverb Comb3 Delay", DEFAULT_REVERB_COMB3_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb3Delay" }, descriptionKey = "REVERB_COMB3_DELAY")
	public void setReverbComb3Delay(float reverbComb3Delay) {
		iniReader.setProperty("Audio", "Reverb Comb3 Delay", reverbComb3Delay);
	}

	@Override
	public float getReverbComb4Delay() {
		return iniReader.getPropertyFloat("Audio", "Reverb Comb4 Delay", DEFAULT_REVERB_COMB4_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb4Delay" }, descriptionKey = "REVERB_COMB4_DELAY")
	public void setReverbComb4Delay(float reverbComb4Delay) {
		iniReader.setProperty("Audio", "Reverb Comb4 Delay", reverbComb4Delay);
	}

	@Override
	public float getReverbAllPass1Delay() {
		return iniReader.getPropertyFloat("Audio", "Reverb All Pass1 Delay", DEFAULT_REVERB_COMB1_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbAllPass1Delay" }, descriptionKey = "REVERB_ALL_PASS1_DELAY")
	public void setReverbAllPass1Delay(float reverbAllPass1Delay) {
		iniReader.setProperty("Audio", "Reverb All Pass1 Delay", reverbAllPass1Delay);
	}

	@Override
	public float getReverbAllPass2Delay() {
		return iniReader.getPropertyFloat("Audio", "Reverb All Pass2 Delay", DEFAULT_REVERB_COMB2_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbAllPass2Delay" }, descriptionKey = "REVERB_ALL_PASS2_DELAY")
	public void setReverbAllPass2Delay(float reverbAllPass2Delay) {
		iniReader.setProperty("Audio", "Reverb All Pass2 Delay", reverbAllPass2Delay);
	}

	@Override
	public float getReverbSustainDelay() {
		return iniReader.getPropertyFloat("Audio", "Reverb Sustain Delay", DEFAULT_REVERB_SUSTAIN_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbSustainDelay" }, descriptionKey = "REVERB_SUSTAIN_DELAY")
	public void setReverbSustainDelay(float reverbSustainDelay) {
		iniReader.setProperty("Audio", "Reverb Sustain Delay", reverbSustainDelay);
	}

	@Override
	public float getReverbDryWetMix() {
		return iniReader.getPropertyFloat("Audio", "Reverb Dry Wet Mix", DEFAULT_REVERB_DRY_WET_MIX);
	}

	@Override
	@Parameter(names = { "--reverbDryWetMix" }, descriptionKey = "REVERB_DRY_WET_MIX")
	public void setReverbDryWetMix(float reverbDryWetMix) {
		iniReader.setProperty("Audio", "Reverb DryWetMix", reverbDryWetMix);
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