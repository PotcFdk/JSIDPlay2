package resid_builder;

import java.nio.IntBuffer;
import java.util.function.IntConsumer;

/**
 * Sound sample consumer consuming sample data while a SID is being clock'ed. A
 * sample value is added to the audio buffer to mix the output of several SIDs
 * together.<BR>
 * <B>Note:</B> To mix several SIDs, all SampleMixer's IntBuffers must wrap the
 * same audio buffer. Additionally, the buffer must be cleared, before the next
 * mixing starts.
 * 
 * @author ken
 *
 */
class SampleMixer implements IntConsumer {
	/**
	 * 
	 * Extends SampleMixer with fade-in/fade-out feature to smoothly
	 * increase/decrease volume.
	 * 
	 * @author ken
	 *
	 */
	static class FadingSampleMixer extends SampleMixer {
		/**
		 * Fade-in/fade-out time in clock ticks.
		 */
		private long fadeInClocks, fadeOutClocks;

		/**
		 * Fade-in/fade-out clock steps until next volume change and current
		 * fade-in and fade-out counters for left and right speaker.
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
			fadeInValL = fadeInStepL = maxVolL != 0 ? fadeInClocks / maxVolL
					: 0;
			fadeInValR = fadeInStepR = maxVolR != 0 ? fadeInClocks / maxVolR
					: 0;
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
			fadeOutValL = fadeOutStepL = maxVolL != 0 ? fadeOutClocks / maxVolL
					: 0;
			fadeOutValR = fadeOutStepR = maxVolR != 0 ? fadeOutClocks / maxVolR
					: 0;
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

	/**
	 * Buffers of mixed sample values for left/right speaker.
	 */
	private IntBuffer bufferL, bufferR;

	/**
	 * Audibility of mixed sample values for left/right speaker.
	 */
	protected int volumeL, volumeR;

	SampleMixer(IntBuffer audioBufferL, IntBuffer audioBufferR) {
		this.bufferL = audioBufferL;
		this.bufferR = audioBufferR;
	}

	public void setVolume(int volumeL, int volumeR) {
		this.volumeL = volumeL;
		this.volumeR = volumeR;
	}

	@Override
	public void accept(int sample) {
		bufferL.put(bufferL.get(bufferL.position()) + sample * volumeL);
		bufferR.put(bufferR.get(bufferR.position()) + sample * volumeR);
	}

	void rewind() {
		bufferL.rewind();
		bufferR.rewind();
	}

}