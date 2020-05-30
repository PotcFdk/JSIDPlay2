package libsidplay.config;

import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
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
	 * @param audio audio to be used
	 */
	void setAudio(Audio audio);

	int getDevice();

	void setDevice(int device);

	/**
	 * Getter of the Playback/Recording frequency.
	 *
	 * @return Playback/Recording frequency
	 */
	SamplingRate getSamplingRate();

	/**
	 * Setter of the sampling rate.
	 *
	 * @param sampligRate sampling rate
	 */
	void setSamplingRate(SamplingRate sampligRate);

	/**
	 * Getter of the sampling method.
	 *
	 * @return the sampling method
	 */
	SamplingMethod getSampling();

	/**
	 * Setter of the sampling method.
	 *
	 * @param method the sampling method
	 */
	void setSampling(SamplingMethod method);

	/**
	 * Getter of the main SID volume setting.
	 *
	 * @return the main SID volume setting
	 */
	float getMainVolume();

	/**
	 * Setter of the main SID volume setting.
	 *
	 * @param volume the main SID volume setting
	 */
	void setMainVolume(float volume);

	/**
	 * Getter of the second SID volume setting.
	 *
	 * @return the second SID volume setting
	 */
	float getSecondVolume();

	/**
	 * Setter of the second SID volume setting.
	 *
	 * @param volume the second SID volume setting
	 */
	void setSecondVolume(float volume);

	/**
	 * Getter of the third SID volume setting.
	 *
	 * @return the third SID volume setting
	 */
	float getThirdVolume();

	/**
	 * Setter of the third SID setting.
	 *
	 * @param volume the third SID volume setting
	 */
	void setThirdVolume(float volume);

	/**
	 * Getter of the main SID balance setting (0 - left, 1 - right speaker).
	 *
	 * @return the main SID balance setting
	 */
	float getMainBalance();

	/**
	 * Setter of the main SID balance setting (0 - left, 1 - right speaker).
	 *
	 * @param balance the main SID balance setting
	 */
	void setMainBalance(float balance);

	/**
	 * Getter of the second SID balance setting (0 - left, 1 - right speaker).
	 *
	 * @return the second SID balance setting
	 */
	float getSecondBalance();

	/**
	 * Setter of the second SID balance setting (0 - left, 1 - right speaker).
	 *
	 * @param balance the second SID balance setting
	 */
	void setSecondBalance(float balance);

	/**
	 * Getter of the third SID balance setting (0 - left, 1 - right speaker).
	 *
	 * @return the third SID balance setting
	 */
	float getThirdBalance();

	/**
	 * Setter of the third SID balance setting (0 - left, 1 - right speaker).
	 *
	 * @param balance the third SID balance setting
	 */
	void setThirdBalance(float balance);

	/**
	 * Getter of the main SID delay setting (0 - no delay, 50 - 50ms delay).
	 *
	 * @return the main SID delay setting
	 */
	int getMainDelay();

	/**
	 * Setter of the main SID delay setting (0 - no delay, 50 - 50ms delay).
	 *
	 * @param delay the main SID delay setting
	 */
	void setMainDelay(int delay);

	/**
	 * Getter of the second SID delay setting (0 - no delay, 50 - 50ms delay).
	 *
	 * @return the second SID delay setting
	 */
	int getSecondDelay();

	/**
	 * Setter of the second SID delay setting (0 - no delay, 50 - 50ms delay).
	 *
	 * @param delay the second SID delay setting
	 */
	void setSecondDelay(int delay);

	/**
	 * Getter of the third SID delay setting (0 - no delay, 50 - 50ms delay).
	 *
	 * @return the third SID delay setting
	 */
	int getThirdDelay();

	/**
	 * Setter of the third SID delay setting (0 - no delay, 50 - 50ms delay).
	 *
	 * @param delay the third SID delay setting
	 */
	void setThirdDelay(int delay);

	/**
	 * Getter of the output buffer size.
	 *
	 * @return size of the output buffer
	 */
	int getBufferSize();

	/**
	 * Setter of the output buffer size.
	 *
	 * @param bufferSize output buffer size
	 */
	void setBufferSize(int bufferSize);

	/**
	 * Getter of the audio buffer size.
	 *
	 * @return size of the audio buffer
	 */
	int getAudioBufferSize();

	/**
	 * Setter of the audio buffer size.
	 *
	 * @param audioBufferSize audio buffer size
	 */
	void setAudioBufferSize(int audioBufferSize);

	/**
	 * Audio Driver: Compare with MP3 recording - do we play the recording?
	 *
	 * @return play the recording
	 */
	boolean isPlayOriginal();

	/**
	 * Audio Driver: compare with MP3 recording - Setter to play the recorded tune.
	 *
	 * @param original Play recorded (original) or emulated tune
	 */
	void setPlayOriginal(boolean original);

	/**
	 * Audio Driver: Compare with MP3 recording - Getter of the recorded tune
	 * filename.
	 *
	 * @return the recorded tune filename
	 */
	String getMp3File();

	/**
	 * Audio Driver: Compare with MP3 recording - Setter of the recorded tune
	 * filename.
	 *
	 * @param recording the recorded tune filename
	 */
	void setMp3File(String recording);

	/**
	 * Audio Driver: MP3 recording - Getter of the constant bit rate.
	 *
	 * @return the constant bit rate
	 */
	int getCbr();

	/**
	 * Audio Driver: MP3 recording - Setter of the constant bit rate.
	 *
	 * @param cbr constant bit rate
	 */
	void setCbr(int cbr);

	/**
	 * Audio Driver: MP3 recording - do we use variable bitrate instead of constant
	 * bitrate?
	 *
	 * @return use variable bitrate
	 */
	boolean isVbr();

	/**
	 * Audio Driver: MP3 recording - use variable bitrate instead of constant
	 * bitrate.
	 *
	 * @param vbr use variable bitrate
	 */
	void setVbr(boolean vbr);

	/**
	 * Audio Driver: MP3 recording - Getter of the variable bitrate quality.
	 *
	 * @return variable bitrate quality
	 */
	int getVbrQuality();

	/**
	 * Audio Driver: MP3 recording - Setter of the variable bitrate quality.
	 *
	 * @param vbr variable bitrate quality
	 */
	void setVbrQuality(int vbr);

	/**
	 * Audio Driver: AVI recording - Getter of the compression quality.
	 *
	 * @return compression quality
	 */
	float getAviCompressionQuality();

	/**
	 * Audio Driver: AVI recording - Setter of the compression quality.
	 *
	 * @param aviCompressionQuality compression quality
	 */
	void setAviCompressionQuality(float aviCompressionQuality);

	/**
	 * Getter of the delay bypass setting
	 *
	 * @return delay bypass setting
	 */
	boolean getDelayBypass();

	/**
	 * Setter of the delay bypass setting
	 *
	 * @param delayBypass delay bypass setting
	 */
	void setDelayBypass(boolean delayBypass);

	/**
	 * Getter of the delay setting
	 *
	 * @return delay setting
	 */
	int getDelay();

	/**
	 * Setter of the delay setting
	 *
	 * @param delay delay setting
	 */
	void setDelay(int delay);

	/**
	 * Getter of the delay wet level setting
	 *
	 * @return delay wet level setting
	 */
	int getDelayWetLevel();

	/**
	 * Setter of the delay wet level setting
	 *
	 * @param delayWetLevel delay wet level setting
	 */
	void setDelayWetLevel(int delayWetLevel);

	/**
	 * Getter of the delay dry level setting
	 *
	 * @return delay dry level setting
	 */
	int getDelayDryLevel();

	/**
	 * Setter of the delay dry level setting
	 *
	 * @param delayDryLevel delay dry level setting
	 */
	void setDelayDryLevel(int delayDryLevel);

	/**
	 * Getter of the delay feedback level setting
	 *
	 * @return delay feedback level setting
	 */
	int getDelayFeedbackLevel();

	/**
	 * Setter of the delay feedback level setting
	 *
	 * @param delayFeedbackLevel delay feedback level setting
	 */
	void setDelayFeedbackLevel(int delayFeedbackLevel);

	/**
	 * Getter of the reverb bypass setting
	 *
	 * @return reverb bypass setting
	 */
	boolean getReverbBypass();

	/**
	 * Setter of the reverb bypass setting
	 *
	 * @param reverbBypass reverb bypass setting
	 */
	void setReverbBypass(boolean reverbBypass);

	/**
	 * Getter of the reverb comp1 delay setting
	 *
	 * @return reverb comp1 delay setting
	 */
	float getReverbComb1Delay();

	/**
	 * Setter of the reverb comp1 delay setting
	 *
	 * @param reverbComb1Delay reverb comp1 delay setting
	 */
	void setReverbComb1Delay(float reverbComb1Delay);

	/**
	 * Getter of the reverb comp2 delay setting
	 *
	 * @return reverb comp2 delay setting
	 */
	float getReverbComb2Delay();

	/**
	 * Setter of the reverb comp2 delay setting
	 *
	 * @param reverbComb2Delay reverb comp2 delay setting
	 */
	void setReverbComb2Delay(float reverbComb2Delay);

	/**
	 * Getter of the reverb comp3 delay setting
	 *
	 * @return reverb comp3 delay setting
	 */
	float getReverbComb3Delay();

	/**
	 * Setter of the reverb comp3 delay setting
	 *
	 * @param reverbComb3Delay reverb comp3 delay setting
	 */
	void setReverbComb3Delay(float reverbComb3Delay);

	/**
	 * Getter of the reverb comp4 delay setting
	 *
	 * @return reverb comp4 delay setting
	 */
	float getReverbComb4Delay();

	/**
	 * Setter of the reverb comp4 delay setting
	 *
	 * @param reverbComb4Delay reverb comp4 delay setting
	 */
	void setReverbComb4Delay(float reverbComb4Delay);

	/**
	 * Getter of the reverb all pass1 delay setting
	 *
	 * @return reverb all pass1 delay setting
	 */
	float getReverbAllPass1Delay();

	/**
	 * Setter of the reverb all pass1 delay delay setting
	 *
	 * @param reverbAllPass1Delay reverb all pass1 delay delay setting
	 */
	void setReverbAllPass1Delay(float reverbAllPass1Delay);

	/**
	 * Getter of the reverb all pass2 delay setting
	 *
	 * @return reverb all pass2 delay setting
	 */
	float getReverbAllPass2Delay();

	/**
	 * Setter of the reverb all pass2 delay delay setting
	 *
	 * @param reverbAllPass2Delay reverb all pass2 delay delay setting
	 */
	void setReverbAllPass2Delay(float reverbAllPass2Delay);

	/**
	 * Getter of the reverb sustain delay setting
	 *
	 * @return reverb sustain delay setting
	 */
	float getReverbSustainDelay();

	/**
	 * Setter of the reverb sustain delay delay setting
	 *
	 * @param reverbSustainDelay reverb sustain delay delay setting
	 */
	void setReverbSustainDelay(float reverbSustainDelay);

	/**
	 * Getter of the reverb dry wet mix setting
	 *
	 * @return reverb dry wet mix setting
	 */
	float getReverbDryWetMix();

	/**
	 * Setter of the reverb dry wet mix setting
	 *
	 * @param reverbDryWetMix reverb dry wet mix setting
	 */
	void setReverbDryWetMix(float reverbDryWetMix);

	default float getVolume(int sidNum) {
		switch (sidNum) {
		case 0:
			return getMainVolume();
		case 1:
			return getSecondVolume();
		case 2:
			return getThirdVolume();
		default:
			throw new RuntimeException(String.format("Maximum supported SIDS exceeded: %d!", sidNum));
		}
	}

	default float getBalance(int sidNum) {
		switch (sidNum) {
		case 0:
			return getMainBalance();
		case 1:
			return getSecondBalance();
		case 2:
			return getThirdBalance();
		default:
			throw new RuntimeException(String.format("Maximum supported SIDS exceeded: %d!", sidNum));
		}
	}

	default int getDelay(int sidNum) {
		switch (sidNum) {
		case 0:
			return getMainDelay();
		case 1:
			return getSecondDelay();
		case 2:
			return getThirdDelay();
		default:
			throw new RuntimeException(String.format("Maximum supported SIDS exceeded: %d!", sidNum));
		}
	}
}