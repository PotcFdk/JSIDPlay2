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
			curInValL, curInValR, curOutValL, curOutValR;

	/**
	 * Currently configured volume level.
	 */
	private int maxVolL, maxVolR;

	FadingSampleMixer(IntBuffer audioBufferL, IntBuffer audioBufferR) {
		super(audioBufferL, audioBufferR);
	}

	public void setVolume(int volumeL, int volumeR) {
		super.setVolume(0, 0);
		this.maxVolL = volumeL;
		this.maxVolR = volumeR;
		updateFader();
	}

	/**
	 * Set fade-in time.
	 * 
	 * @param fadeInClocks
	 *            fade-in time in clock ticks (0 means disabled)
	 */
	public void setFadeInClocks(long fadeInClocks) {
		this.fadeInClocks = fadeInClocks;
		updateFader();
	}

	/**
	 * Set fade-out time.
	 * 
	 * @param fadeOutClocks
	 *            fade-out time in clock ticks (0 means disabled)
	 */
	public void setFadeOutClocks(long fadeOutClocks) {
		this.fadeOutClocks = fadeOutClocks;
		updateFader();
	}

	/**
	 * Volume increment with respect to fade-in/fade-out.
	 */
	private void updateFader() {
		curInValL = fadeInStepL = maxVolL != 0 ? fadeInClocks / maxVolL : 0;
		curInValR = fadeInStepR = maxVolR != 0 ? fadeInClocks / maxVolR : 0;
		curOutValL = fadeOutStepL = maxVolL != 0 ? fadeOutClocks / maxVolL : 0;
		curOutValR = fadeOutStepR = maxVolR != 0 ? fadeOutClocks / maxVolR : 0;
	}

	@Override
	public void accept(int sample) {
		if (fadeInClocks >= 0) {
			if (fadeInClocks-- == 0) {
				// no fade-in? Initially set volume
				volumeL = maxVolL;
				volumeR = maxVolR;
			} else {
				if (--curInValL == 0) {
					curInValL = fadeInStepL;
					volumeL++;
				}
				if (--curInValR == 0) {
					curInValR = fadeInStepR;
					volumeR++;
				}
			}
		} else if (fadeOutClocks > 0) {
			if (--curOutValL == 0) {
				curOutValL = fadeOutStepL;
				volumeL--;
			}
			if (--curOutValR == 0) {
				curOutValR = fadeOutStepR;
				volumeR--;
			}
		}
		super.accept(sample);
	}

}