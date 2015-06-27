package resid_builder;

import java.nio.IntBuffer;

/**
 * 
 * Extends SampleMixer with fade-in/fade-out feature to smoothly
 * increase/decrease volume.
 * 
 * @author ken
 *
 */
class FadingSampleMixer extends SampleMixer {
	/**
	 * Fade-in/fade-out time in clock ticks.
	 */
	private long fadeInClocks, fadeOutClocks;

	/**
	 * Fade-in/fade-out clock steps until next volume change and current fade-in
	 * and fade-out counters for left and right speaker.
	 */
	private long fadeInStepL, fadeInStepR, fadeOutStepL, fadeOutStepR,
			fadeInValL, fadeInValR, fadeOutValL, fadeOutValR;

	/**
	 * Currently configured volume level.
	 */
	private int maxVolL, maxVolR;

	FadingSampleMixer(IntBuffer audioBufferL, IntBuffer audioBufferR) {
		super(audioBufferL, audioBufferR);
	}

	/**
	 * Set fade-in time. Increae volume from zero to the maximum.
	 * 
	 * @param fadeIn
	 *            fade-in time in clock ticks
	 */
	public void setFadeIn(long fadeIn) {
		this.fadeInClocks = fadeIn;
		this.maxVolL = volumeL;
		this.maxVolR = volumeR;
		super.setVolume(0, 0);
		updateFader();
	}

	/**
	 * Set fade-out time. Decrease volume from the maximum to zero.
	 * 
	 * @param fadeOut
	 *            fade-out time in clock ticks
	 */
	public void setFadeOut(long fadeOut) {
		this.fadeOutClocks = fadeOut;
		super.setVolume(maxVolL, maxVolR);
		updateFader();
	}

	/**
	 * Calculate after how many clock ticks volume must be increased/decreased.
	 */
	private void updateFader() {
		fadeInValL = fadeInStepL = maxVolL != 0 ? fadeInClocks / maxVolL : 0;
		fadeInValR = fadeInStepR = maxVolR != 0 ? fadeInClocks / maxVolR : 0;
		fadeOutValL = fadeOutStepL = maxVolL != 0 ? fadeOutClocks / maxVolL : 0;
		fadeOutValR = fadeOutStepR = maxVolR != 0 ? fadeOutClocks / maxVolR : 0;
	}

	@Override
	public void accept(int sample) {
		if (fadeInClocks > 0) {
			fadeInClocks--;
			if (--fadeInValL == 0) {
				fadeInValL = fadeInStepL;
				volumeL++;
			}
			if (--fadeInValR == 0) {
				fadeInValR = fadeInStepR;
				volumeR++;
			}
		} else if (fadeOutClocks > 0) {
			fadeOutClocks--;
			if (--fadeOutValL == 0) {
				fadeOutValL = fadeOutStepL;
				volumeL--;
			}
			if (--fadeOutValR == 0) {
				fadeOutValR = fadeOutStepR;
				volumeR--;
			}
		}
		super.accept(sample);
	}

}