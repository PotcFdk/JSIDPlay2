package resid_builder;

import java.nio.IntBuffer;
import java.util.function.IntConsumer;

/**
 * Sound sample consumer consuming sample data while a SID is being clock'ed. A
 * sample value is added to the audio buffer to mix the output of several SIDs
 * together.<BR>
 * Note: To mix several SIDs, all SampleMixer's IntBuffers must wrap the same
 * audio buffer. Additionally, the buffer must be cleared, before the next
 * mixing starts.
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