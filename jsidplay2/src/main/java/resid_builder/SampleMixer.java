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
 * Fade-in/fade-out can be set to smootly increase/decrease vlolume.
 * 
 * @author ken
 *
 */
class SampleMixer implements IntConsumer {
	/**
	 * Buffers of mixed sample values for left/right speaker.
	 */
	private IntBuffer bufferL, bufferR;

	/**
	 * Audibility of mixed sample values for left/right speaker.
	 */
	private int volumeL, volumeR;

	/**
	 * Fade-in&fade-out time in clock ticks.
	 */
	private long fadeIn, fadeOut;

	/**
	 * Currently faded volume level, fade-in increment and fade-out decrement
	 * for left and right speaker as doubles.
	 */
	private double fadedVolumeL, fadedVolumeR, volumeIncL, volumeIncR,
			volumeDecL, volumeDecR;

	/**
	 * Currently faded volume level.
	 */
	private int currVolumeL, currVolumeR;

	SampleMixer(IntBuffer audioBufferL, IntBuffer audioBufferR) {
		this.bufferL = audioBufferL;
		this.bufferR = audioBufferR;
	}

	public void setVolume(int volumeL, int volumeR) {
		this.volumeL = volumeL;
		this.volumeR = volumeR;
		updateVolumeIncrement();
	}

	/**
	 * Set fade-in time.
	 * 
	 * @param fadeIn
	 *            fade-in time in clock ticks
	 */
	public void setFadeIn(long fadeIn) {
		assert fadeIn >= 0;
		this.fadeIn = fadeIn;
		updateVolumeIncrement();
	}

	/**
	 * Set fade-out time.
	 * 
	 * @param fadeOut
	 *            fade-out time in clock ticks
	 */
	public void setFadeOut(long fadeOut) {
		assert fadeOut >= 0;
		this.fadeOut = fadeOut;
		updateVolumeIncrement();
	}

	/**
	 * Volume increment with respect to fade-in/fade-out.
	 * 
	 * <B>Note:</B> If fadeIn==0? Increment volume to the max next time.
	 */
	private void updateVolumeIncrement() {
		volumeIncL = fadeIn != 0 ? (double) volumeL / fadeIn : (double) volumeL;
		volumeIncR = fadeIn != 0 ? (double) volumeR / fadeIn : (double) volumeR;
		volumeDecL = fadeOut != 0 ? (double) volumeL / fadeOut : 0;
		volumeDecR = fadeOut != 0 ? (double) volumeR / fadeOut : 0;
	}

	@Override
	public void accept(int sample) {
		if (fadeIn >= 0) {
			fadeIn--;
			// fade-in (fadeIn==0? set maximum volume)
			currVolumeL = (int) Math.round(fadedVolumeL += volumeIncL);
			currVolumeR = (int) Math.round(fadedVolumeR += volumeIncR);
		}
		if (fadeOut > 0) {
			fadeOut--;
			// fade-out
			currVolumeL = (int) Math.round(fadedVolumeL -= volumeDecL);
			currVolumeR = (int) Math.round(fadedVolumeR -= volumeDecR);
		}
		bufferL.put(bufferL.get(bufferL.position()) + sample * currVolumeL);
		bufferR.put(bufferR.get(bufferR.position()) + sample * currVolumeR);
	}

	void rewind() {
		bufferL.rewind();
		bufferR.rewind();
	}

}