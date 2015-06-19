package resid_builder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.components.pla.PLA;
import resid_builder.resample.Resampler;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;

/**
 * Mixer to mix SIDs sample data into the audio buffer.
 * 
 * @author ken
 *
 */
public class SIDMixer {
	/**
	 * Maximum fast forward factor (1 << 5 = 32).
	 */
	public static final int MAX_FAST_FORWARD = 5;

	/**
	 * NullAudio ignores generated sound samples. This is used, before timer
	 * start has been reached.
	 * 
	 * @author ken
	 *
	 */
	private final class NullAudioEvent extends Event {
		private NullAudioEvent(String name) {
			super(name);
		}

		@Override
		public void event() throws InterruptedException {
			for (ReSIDBase sid : sids) {
				SampleMixer sampler = (SampleMixer) sid.getSampler();
				// clock SID to the present moment
				sid.clock();
				// rewind
				sampler.rewind();
			}
			context.schedule(this, audioBufferL.capacity());
		}
	}

	/**
	 * The mixer mixes the generated sound samples into the drivers audio
	 * buffer. This is used, after timer start has been reached.
	 * 
	 * @author ken
	 *
	 */
	private final class MixerEvent extends Event {
		private MixerEvent(String name) {
			super(name);
		}

		/**
		 * Random source for triangular dithering
		 */
		private Random RANDOM = new Random();
		/**
		 * State of HP-TPDF.
		 */
		private int oldRandomValue;

		@Override
		public void event() throws InterruptedException {
			// Clock SIDs to fill audio buffer
			for (ReSIDBase sid : sids) {
				SampleMixer sampler = (SampleMixer) sid.getSampler();
				// clock SID to the present moment
				sid.clock();
				// rewind
				sampler.rewind();
			}
			int len = isFastForward()? fastForward() : audioBufferL.capacity();
			// Output sample data
			for (int pos = 0; pos < len; pos++) {
				int dither = triangularDithering();

				putSample(resamplerL, audioBufferL.get(pos), dither);
				putSample(resamplerR, audioBufferR.get(pos), dither);
				audioBufferL.put(pos, 0);
				audioBufferR.put(pos, 0);

				if (!driver.buffer().hasRemaining()) {
					driver.write();
					driver.buffer().clear();
				}
			}
			context.schedule(this, audioBufferL.capacity());
		}

		/**
		 * Fast forward tune by accumulation of sample data
		 * 
		 * @return new audio buffer length (len/fastForward)
		 */
		private int fastForward() {
			int newLen, valL, valR, factor;
			newLen = valL = valR = factor = 0;
			for (int pos = 0; pos < audioBufferL.capacity(); pos++) {
				// accumulate each interleaved channel
				valL += audioBufferL.get(pos);
				valR += audioBufferR.get(pos);
				audioBufferL.put(pos, 0);
				audioBufferR.put(pos, 0);
				// once enough samples have been accumulated, write output
				if (++factor == (1 << fastForward)) {
					audioBufferL.put(newLen, valL >> fastForward);
					audioBufferR.put(newLen++, valR >> fastForward);
					// zero accumulator
					valL = valR = factor = 0;
				}
			}
			return newLen;
		}

		/**
		 * <OL>
		 * <LI>Resample the SID output, because the sample frequency is
		 * different to the clock frequency .
		 * <LI>Add dithering to reduce quantization noise, when moving to a
		 * format with less precision.
		 * <LI>Cut-off overflow samples.
		 * </OL>
		 * 
		 * @param resampler
		 *            resampler
		 * @param value
		 *            sample value
		 * @param dither
		 *            triangularly shaped noise
		 */
		private final void putSample(Resampler resampler, int value, int dither) {
			if (resampler.input(value >> 10)) {
				value = resampler.output() + dither;
				if (value > 32767) {
					value = 32767;
				}
				if (value < -32768) {
					value = -32768;
				}
				driver.buffer().putShort((short) value);
			}
		}

		/**
		 * Triangularly shaped noise source for audio applications. Output of
		 * this PRNG is between ]-1, 1[.
		 * 
		 * @return triangular noise sample
		 */
		private int triangularDithering() {
			int prevValue = oldRandomValue;
			oldRandomValue = RANDOM.nextInt() & 0x1;
			return oldRandomValue - prevValue;
		}

	}

	/**
	 * System event context.
	 */
	private EventScheduler context;

	/**
	 * Configuration
	 */
	private IConfig config;

	/**
	 * Mixer WITHOUT audio output, just clocking SID chips.
	 */
	private Event nullAudio = new NullAudioEvent("NullAudio");
	/**
	 * Mixer clocking SID chips and producing audio output.
	 */
	private Event mixerAudio = new MixerEvent("MixerAudio");

	/**
	 * SIDs to mix their sound output.
	 */
	private List<ReSIDBase> sids = new ArrayList<ReSIDBase>();

	/**
	 * Audio buffer for two channels (stereo).
	 */
	private IntBuffer audioBufferL, audioBufferR;

	/**
	 * Resampler of sample output for two channels (stereo).
	 */
	private Resampler resamplerL, resamplerR;

	/**
	 * Audio driver
	 */
	private AudioDriver driver;

	/**
	 * Volume of all SIDs.
	 */
	private int[] volume = new int[PLA.MAX_SIDS];
	/**
	 * SID audibility on the left speaker of all SIDs 0(silent)..1(loud).
	 */
	private float[] positionL = new float[PLA.MAX_SIDS];
	/**
	 * SID audibility on the right speaker of all SIDs 0(silent)..1(loud).
	 */
	private float[] positionR = new float[PLA.MAX_SIDS];

	/**
	 * Fast forward factor (1 << fastForward).
	 */
	private int fastForward;

	public SIDMixer(EventScheduler context, IConfig config, CPUClock cpuClock,
			AudioConfig audioConfig, AudioDriver audioDriver) {
		this.context = context;
		this.config = config;
		this.driver = audioDriver;
		IAudioSection audioSection = config.getAudioSection();
		this.audioBufferL = ByteBuffer
				.allocateDirect(Integer.BYTES * audioSection.getBufferSize())
				.order(ByteOrder.nativeOrder()).asIntBuffer();
		this.audioBufferR = ByteBuffer
				.allocateDirect(Integer.BYTES * audioSection.getBufferSize())
				.order(ByteOrder.nativeOrder()).asIntBuffer();
		this.resamplerL = Resampler.createResampler(cpuClock.getCpuFrequency(),
				audioSection.getSampling(), audioConfig.getFrameRate(), 20000);
		this.resamplerR = Resampler.createResampler(cpuClock.getCpuFrequency(),
				audioSection.getSampling(), audioConfig.getFrameRate(), 20000);
	}

	public void reset() {
		context.schedule(nullAudio, 0, Event.Phase.PHI2);
	}

	/**
	 * Starts mixing the outputs of several SIDs. Write samples to the sound
	 * buffer.
	 */
	public void start() {
		context.cancel(nullAudio);
		context.schedule(mixerAudio, 0, Event.Phase.PHI2);
	}

	/**
	 * Add a SID to the mix.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param sid
	 *            SID to add
	 */
	public void add(int sidNum, ReSIDBase sid) {
		if (sidNum < sids.size()) {
			sids.set(sidNum, sid);
		} else {
			sids.add(sid);
		}
		createSampleMixer(sid);
		setVolume(sidNum);
		setBalance(sidNum);
	}

	/**
	 * Remove SID from the mix.
	 * 
	 * @param sid
	 *            SID to remove
	 */
	public void remove(SIDEmu sid) {
		sids.remove(sid);
		setSampleMixerVolume();
	}

	/**
	 * Getter for SIDs in the mix.
	 * 
	 * @return SIDs in the mix
	 */
	public List<ReSIDBase> getSIDs() {
		return sids;
	}

	/**
	 * @return current number of SIDs.
	 */
	public int getSIDCount() {
		return sids.size();
	}

	/**
	 * Volume of the SID chip.<BR>
	 * -6(-6db)..6(+6db)
	 * 
	 * @param sidNum
	 *            SID chip number
	 */
	public void setVolume(int sidNum) {
		float volumeInDB = config.getAudioSection().getVolume(sidNum);
		assert volumeInDB >= -6 && volumeInDB <= 6;

		volume[sidNum] = (int) (Math.pow(10, volumeInDB / 10) * 1024);
		setSampleMixerVolume();
	}

	/**
	 * Set left/right speaker balance for each SID.<BR>
	 * 0(left speaker)..0.5(centered)..1(right speaker)
	 * 
	 * @param sidNum
	 *            SID chip number
	 */
	public void setBalance(int sidNum) {
		float balance = config.getAudioSection().getBalance(sidNum);
		assert balance >= 0 && balance <= 1;

		positionL[sidNum] = 1 - balance;
		positionR[sidNum] = balance;
		setSampleMixerVolume();
	}

	/**
	 * Create a new sample value mixer and assign to SID chip.
	 * 
	 * @param sid
	 *            SID chip that requires a sample mixer.
	 */
	private void createSampleMixer(ReSIDBase sid) {
		IntBuffer intBufferL = audioBufferL.duplicate();
		IntBuffer intBufferR = audioBufferR.duplicate();
		sid.setSampler(new SampleMixer(intBufferL, intBufferR));
	}

	/**
	 * Set the sample mixer volume to the calculated balanced volume level.<BR>
	 * Mono output: Use volume.<BR>
	 * Stereo or 3-SID output: Use speaker audibility and volume.
	 */
	private void setSampleMixerVolume() {
		boolean stereo = sids.size() > 1;
		int sidNum = 0;
		for (ReSIDBase sid : sids) {
			SampleMixer sampler = (SampleMixer) sid.getSampler();
			if (stereo) {
				int volumeL = (int) (volume[sidNum] * positionL[sidNum]);
				int volumeR = (int) (volume[sidNum] * positionR[sidNum]);
				sampler.setVolume(volumeL, volumeR);
			} else {
				sampler.setVolume(volume[sidNum], volume[sidNum]);
			}
			sidNum++;
		}
	}

	/**
	 * Doubles speed factor.
	 */
	public void fastForward() {
		fastForward++;
		if (fastForward > MAX_FAST_FORWARD) {
			fastForward = MAX_FAST_FORWARD;
		}
	}

	/**
	 * Use normal speed factor.
	 */
	public void normalSpeed() {
		fastForward = 0;
	}

	public boolean isFastForward() {
		return fastForward != 0;
	}

}