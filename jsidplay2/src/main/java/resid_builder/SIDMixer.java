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
import libsidplay.common.Mixer;
import libsidplay.common.SIDEmu;
import libsidplay.components.pla.PLA;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import resid_builder.SampleMixer.FadingSampleMixer;
import resid_builder.resample.Resampler;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

/**
 * Mixer to mix SIDs sample data into the audio buffer.
 * 
 * @author ken
 *
 */
public class SIDMixer implements Mixer {
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
			// Accumulate sample data with respect to fast forward factor
			int valL = 0, valR = 0, factor = 0;
			for (int pos = 0; pos < audioBufferL.capacity(); pos++) {
				valL += audioBufferL.get(pos);
				valR += audioBufferR.get(pos);
				audioBufferL.put(pos, 0);
				audioBufferR.put(pos, 0);

				// once enough samples have been accumulated, write output
				if (++factor == 1 << fastForward) {
					int dither = triangularDithering();

					putSample(resamplerL, valL >> fastForward, dither);
					putSample(resamplerR, valR >> fastForward, dither);
					if (!driver.buffer().hasRemaining()) {
						driver.write();
						driver.buffer().clear();
					}
					// zero accumulator
					valL = valR = factor = 0;
				}
			}
			context.schedule(this, audioBufferL.capacity());
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
	protected EventScheduler context;

	/**
	 * Configuration
	 */
	protected IConfig config;

	/**
	 * CPU clock.
	 */
	protected CPUClock cpuClock;

	/**
	 * SIDs to mix their sound output.
	 */
	protected List<ReSIDBase> sids = new ArrayList<ReSIDBase>();

	/**
	 * Mixer WITHOUT audio output, just clocking SID chips.
	 */
	private Event nullAudio = new NullAudioEvent("NullAudio");
	/**
	 * Mixer clocking SID chips and producing audio output.
	 */
	private Event mixerAudio = new MixerEvent("MixerAudio");

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

	/**
	 * Fade-in/fade-out enabled.
	 */
	private boolean fadeInFadeOutEnabled;

	public SIDMixer(EventScheduler context, IConfig config, CPUClock cpuClock,
			AudioConfig audioConfig, AudioDriver audioDriver) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
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
		this.fadeInFadeOutEnabled = config.getSidplay2Section().getFadeInTime() != 0
				|| config.getSidplay2Section().getFadeOutTime() != 0;
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
	 * Fade-in start time reached, audio volume should be increased to the max.
	 * 
	 * @param fadeIn
	 *            Fade-in time in seconds
	 */
	public void fadeIn(int fadeIn) {
		for (ReSIDBase sid : sids) {
			FadingSampleMixer sampler = (FadingSampleMixer) sid.getSampler();
			sampler.setFadeIn((long) (fadeIn * cpuClock.getCpuFrequency()));
		}
	}

	/**
	 * Fade-out start time reached, audio volume should be lowered to zero.
	 * 
	 * @param fadeOut
	 *            Fade-out time in seconds
	 */
	public void fadeOut(int fadeOut) {
		for (ReSIDBase sid : sids) {
			FadingSampleMixer sampler = (FadingSampleMixer) sid.getSampler();
			sampler.setFadeOut((long) (fadeOut * cpuClock.getCpuFrequency()));
		}
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
		setVolume(sidNum, config.getAudioSection().getVolume(sidNum));
		setBalance(sidNum, config.getAudioSection().getBalance(sidNum));
	}

	/**
	 * Remove SID from the mix.
	 * 
	 * @param sid
	 *            SID to remove
	 */
	public void remove(SIDEmu sid) {
		sids.remove(sid);
		updateSampleMixerVolume();
	}

	/**
	 * Volume of the SID chip.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param volume
	 *            volume in DB -6(-6db)..6(+6db)
	 */
	public void setVolume(int sidNum, float volumeInDB) {
		assert volumeInDB >= -6 && volumeInDB <= 6;

		volume[sidNum] = (int) (Math.pow(10, volumeInDB / 10) * 1024);
		updateSampleMixerVolume();
	}

	/**
	 * Set left/right speaker balance for each SID.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param balance
	 *            balance 0(left speaker)..0.5(centered)..1(right speaker)
	 */
	public void setBalance(int sidNum, float balance) {
		assert balance >= 0 && balance <= 1;

		positionL[sidNum] = 1 - balance;
		positionR[sidNum] = balance;
		updateSampleMixerVolume();
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
		if (fadeInFadeOutEnabled) {
			sid.setSampler(new FadingSampleMixer(intBufferL, intBufferR));
		} else {
			sid.setSampler(new SampleMixer(intBufferL, intBufferR));
		}
	}

	/**
	 * Set the sample mixer volume to the calculated balanced volume level.<BR>
	 * Mono output: Use volume.<BR>
	 * Stereo or 3-SID output: Use speaker audibility and volume.
	 */
	private void updateSampleMixerVolume() {
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
		if (++fastForward > MAX_FAST_FORWARD) {
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