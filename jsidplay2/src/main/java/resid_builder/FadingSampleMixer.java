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
	private long fadeIn, fadeOut;

	/**
	 * Currently faded volume level, fade-in increment and fade-out decrement
	 * for left and right speaker as doubles.
	 */
	private double fadedVolL, fadedVolR, volIncL, volIncR, volDecL, volDecR;

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
		updateVolumeIncrement();
	}

	/**
	 * Set fade-in time.
	 * 
	 * @param fadeIn
	 *            fade-in time in seconds
	 */
	public void setFadeIn(long fadeIn) {
		assert fadeIn >= 0;
		this.fadeIn = fadeIn * 1000000L;
		updateVolumeIncrement();
	}

	/**
	 * Set fade-out time.
	 * 
	 * @param fadeOut
	 *            fade-out time in seconds
	 */
	public void setFadeOut(long fadeOut) {
		assert fadeOut >= 0;
		this.fadeOut = fadeOut * 1000000L;
		updateVolumeIncrement();
	}

	/**
	 * Volume increment with respect to fade-in/fade-out.
	 * 
	 * <B>Note:</B> If fadeIn==0? Increment volume to the max next event.
	 */
	private void updateVolumeIncrement() {
		volIncL = fadeIn != 0 ? (double) maxVolL / fadeIn : (double) maxVolL;
		volIncR = fadeIn != 0 ? (double) maxVolR / fadeIn : (double) maxVolR;
		volDecL = fadeOut != 0 ? (double) maxVolL / fadeOut : 0;
		volDecR = fadeOut != 0 ? (double) maxVolR / fadeOut : 0;
	}

	@Override
	public void accept(int sample) {
		if (fadeIn >= 0) {
			fadeIn--;
			// fade-in (fadeIn==0? Increment to maximum volume)
			volumeL = (int) Math.round(fadedVolL += volIncL);
			volumeR = (int) Math.round(fadedVolR += volIncR);
		} else if (fadeOut > 0) {
			fadeOut--;
			// fade-out
			volumeL = (int) Math.round(fadedVolL -= volDecL);
			volumeR = (int) Math.round(fadedVolR -= volDecR);
		}
		super.accept(sample);
	}

}