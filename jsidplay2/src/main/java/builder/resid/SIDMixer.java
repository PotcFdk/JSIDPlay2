package builder.resid;

import static java.nio.ByteOrder.nativeOrder;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import builder.resid.SampleMixer.LinearFadingSampleMixer;
import builder.resid.resample.Resampler;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SamplingMethod;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;
import sidplay.audio.AudioDriver;
import sidplay.audio.processor.AudioProcessor;
import sidplay.audio.processor.delay.DelayProcessor;
import sidplay.audio.processor.reverb.ReverbProcessor;
import sidplay.fingerprinting.WhatsSidSupport;

/**
 * Mixer to mix SIDs sample data into the audio buffer.
 *
 * @author ken
 *
 */
public class SIDMixer implements Mixer {
	/**
	 * Scaler to use fast int multiplication while setting volume.
	 */
	private static final int VOLUME_SCALER = 10;

	/**
	 * The mixer mixes the generated sound samples into the drivers audio buffer.
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
		private final Random RANDOM = new Random();
		/**
		 * State of HP-TPDF.
		 */
		private int oldRandomValue;

		/**
		 * Fast forward factor:<BR>
		 * fastForwardShift=1<<(VOLUME_SCALER+fastForwardFactor)
		 */
		private int fastForwardShift, fastForwardBitMask;

		/**
		 * The mixer mixes the generated sound samples into the drivers audio buffer.
		 * <OL>
		 * <LI>Clock SIDs to fill audio buffer.
		 * <LI>Accumulate samples to implement fast forwarding.
		 * <LI>Resample the SID output, because the sample frequency is different to the
		 * clock frequency.
		 * <LI>Add dithering to reduce quantization noise, when moving to a format with
		 * less precision.
		 * <LI>Cut-off overflow samples.
		 * <LI>do some audio post-processing
		 * </OL>
		 * <B>Note:</B><BR>
		 * Audio buffer is cleared afterwards to get refilled during next event.
		 */
		@Override
		public void event() throws InterruptedException {
			// Clock SIDs to fill the audio buffer
			for (ReSIDBase sid : sids) {
				SampleMixer sampler = (SampleMixer) sid.getSampler();
				// clock SID to the present moment
				sid.clock();
				sampler.clear();
			}
			// Read from audio buffers
			int valL = 0, valR = 0;
			for (int i = 0; i < bufferSize; i++) {
				// Accumulate sample data with respect to fast forward factor
				valL += audioBufferL.get();
				valR += audioBufferR.get();

				// once enough samples have been accumulated, write output
				if ((i & fastForwardBitMask) == fastForwardBitMask) {
					int dither = triangularDithering();

					if (resamplerL.input(valL >> fastForwardShift)) {
						short outputL = (short) Math.max(Math.min(resamplerL.output() + dither, Short.MAX_VALUE),
								Short.MIN_VALUE);
						buffer.putShort(outputL);
					}
					if (resamplerR.input(valR >> fastForwardShift)) {
						short outputR = (short) Math.max(Math.min(resamplerR.output() + dither, Short.MAX_VALUE),
								Short.MIN_VALUE);
						if (!buffer.putShort(outputR).hasRemaining()) {
							audioProcessors.stream().forEach(processor -> processor.process(buffer));
							audioDriver.write();
							((Buffer) buffer).clear();
						}
					}
					if (whatsSidEnabled) {
						whatsSidSupport.output(valL >> fastForwardShift, valR >> fastForwardShift);
					}
					// zero accumulator
					valL = valR = 0;
				}
			}
			// Erase audio buffers
			((Buffer) audioBufferL).flip();
			((Buffer) audioBufferR).flip();
			((Buffer) audioBufferL.put(new int[bufferSize])).clear();
			((Buffer) audioBufferR.put(new int[bufferSize])).clear();
			context.schedule(this, bufferSize);
		}

		/**
		 * Triangularly shaped noise source for audio applications. Output of this PRNG
		 * is between ]-1, 1[.
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
	protected final EventScheduler context;

	/**
	 * Configuration
	 */
	protected final IConfig config;

	/**
	 * CPU clock.
	 */
	protected final CPUClock cpuClock;

	/**
	 * SIDs to mix their sound output.
	 */
	protected final List<ReSIDBase> sids = new ArrayList<>(MAX_SIDS);

	/**
	 * Mixer clocking SID chips and producing audio output.
	 */
	private final MixerEvent mixerAudio = new MixerEvent("MixerAudio");

	/**
	 * Audio buffers for two channels (stereo).
	 */
	private IntBuffer audioBufferL, audioBufferR;

	/**
	 * Capacity of the Audio buffers audioBufferL and audioBufferR.
	 */
	private int bufferSize;

	/**
	 * Resampler of sample output for two channels (stereo).
	 */
	private final Resampler resamplerL, resamplerR;

	/**
	 * Audio driver
	 */
	private AudioDriver audioDriver;

	/**
	 * Volume of all SIDs.
	 */
	private final int[] volume = new int[MAX_SIDS];
	/**
	 * SID audibility on the left speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionL = new float[MAX_SIDS];
	/**
	 * SID audibility on the right speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionR = new float[MAX_SIDS];
	/**
	 * Delay in samples of all SIDs.
	 */
	private final int[] delayInSamples = new int[MAX_SIDS];

	/**
	 * Fade-in/fade-out enabled.
	 */
	private final boolean fadeInFadeOutEnabled;

	/**
	 * WhatsSid enabled?
	 */
	private boolean whatsSidEnabled;

	/**
	 * WhatsSID
	 */
	private final WhatsSidSupport whatsSidSupport;

	/**
	 * Add some audio post processing.
	 */
	private List<AudioProcessor> audioProcessors = new ArrayList<>();

	/**
	 * Audio driver buffer.
	 */
	private ByteBuffer buffer;

	public SIDMixer(EventScheduler context, IConfig config, CPUClock cpuClock) {
		ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		IAudioSection audioSection = config.getAudioSection();
		IWhatsSidSection whatsSidSection = config.getWhatsSidSection();

		double cpuFrequency = cpuClock.getCpuFrequency();
		SamplingMethod samplingMethod = audioSection.getSampling();
		int samplingFrequency = audioSection.getSamplingRate().getFrequency();
		int middleFrequency = audioSection.getSamplingRate().getMiddleFrequency();

		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		this.resamplerL = Resampler.createResampler(cpuFrequency, samplingMethod, samplingFrequency, middleFrequency);
		this.resamplerR = Resampler.createResampler(cpuFrequency, samplingMethod, samplingFrequency, middleFrequency);
		this.fadeInFadeOutEnabled = sidplay2Section.getFadeInTime() != 0 || sidplay2Section.getFadeOutTime() != 0;
		this.audioProcessors.add(new DelayProcessor(config));
		this.audioProcessors.add(new ReverbProcessor(config));
		this.whatsSidEnabled = whatsSidSection.isEnable();
		this.whatsSidSupport = new WhatsSidSupport(cpuFrequency, whatsSidSection.getCaptureTime(),
				whatsSidSection.getMinimumRelativeConfidence());

		normalSpeed();
	}

	@Override
	public void setAudioDriver(AudioDriver audioDriver) {
		IAudioSection audioSection = config.getAudioSection();

		this.audioDriver = audioDriver;
		this.bufferSize = audioSection.getBufferSize();
		this.buffer = audioDriver.buffer();
		this.audioBufferL = ByteBuffer.allocateDirect(Integer.BYTES * bufferSize).order(nativeOrder()).asIntBuffer();
		this.audioBufferR = ByteBuffer.allocateDirect(Integer.BYTES * bufferSize).order(nativeOrder()).asIntBuffer();
	}

	/**
	 * Starts mixing the outputs of several SIDs.
	 */
	@Override
	public void start() {
		context.schedule(mixerAudio, 0, Event.Phase.PHI2);
	}

	/**
	 * Fade-in start time reached, audio volume should be increased to the max.
	 *
	 * @param fadeIn Fade-in time in seconds
	 */
	@Override
	public void fadeIn(double fadeIn) {
		for (ReSIDBase sid : sids) {
			LinearFadingSampleMixer sampler = (LinearFadingSampleMixer) sid.getSampler();
			sampler.setFadeIn((long) (fadeIn * cpuClock.getCpuFrequency()));
		}
	}

	/**
	 * Fade-out start time reached, audio volume should be lowered to zero.
	 *
	 * @param fadeOut Fade-out time in seconds
	 */
	@Override
	public void fadeOut(double fadeOut) {
		for (ReSIDBase sid : sids) {
			LinearFadingSampleMixer sampler = (LinearFadingSampleMixer) sid.getSampler();
			sampler.setFadeOut((long) (fadeOut * cpuClock.getCpuFrequency()));
		}
	}

	/**
	 * Add a SID to the mix.
	 *
	 * @param sidNum SID chip number
	 * @param sid    SID to add
	 */
	public void add(int sidNum, ReSIDBase sid) {
		if (sidNum < sids.size()) {
			sids.set(sidNum, sid);
		} else {
			sids.add(sid);
		}
		createSampleMixer(sid, sidNum);
		setVolume(sidNum, config.getAudioSection().getVolume(sidNum));
		setBalance(sidNum, config.getAudioSection().getBalance(sidNum));
		setDelay(sidNum, config.getAudioSection().getDelay(sidNum));
	}

	/**
	 * Remove SID from the mix.
	 *
	 * @param sid SID to remove
	 */
	public void remove(ReSIDBase sid) {
		sids.remove(sid);
		sid.setSampler(null);
		updateSampleMixerVolume();
	}

	/**
	 * Volume of the SID chip.
	 *
	 * @param sidNum     SID chip number
	 * @param volumeInDB volume in DB -6(-6db)..6(+6db)
	 */
	@Override
	public void setVolume(int sidNum, float volumeInDB) {
		assert volumeInDB >= -6 && volumeInDB <= 6;

		volume[sidNum] = (int) (DECIBEL_TO_LINEAR(volumeInDB) * (1 << VOLUME_SCALER));
		updateSampleMixerVolume();
	}

	/**
	 * Convert db to linear.
	 *
	 * @param decibel decibel value to convert
	 * @return converted linear value
	 */
	public static double DECIBEL_TO_LINEAR(float decibel) {
		return Math.pow(10., decibel / 12.);
	}

	/**
	 * Set left/right speaker balance for each SID.
	 *
	 * @param sidNum  SID chip number
	 * @param balance balance 0(left speaker)..0.5(centered)..1(right speaker)
	 */
	@Override
	public void setBalance(int sidNum, float balance) {
		assert balance >= 0 && balance <= 1;

		positionL[sidNum] = (1 - balance);
		positionR[sidNum] = balance;
		updateSampleMixerVolume();
	}

	/**
	 * Delay feature: Delaying SID chip sound samples by time in milliseconds
	 *
	 * @param sidNum SID chip number
	 * @param delay  delay in ms
	 */
	@Override
	public void setDelay(int sidNum, int delay) {
		assert delay >= 0 && delay <= 100;

		delayInSamples[sidNum] = (int) (cpuClock.getCpuFrequency() / 1000. * delay);
		updateSampleMixerVolume();
	}

	/**
	 * Create a new sample value mixer and assign to SID chip.
	 *
	 * @param sid SID chip that requires a sample mixer.
	 */
	private void createSampleMixer(ReSIDBase sid, int sidNum) {
		IntBuffer intBufferL = audioBufferL.duplicate();
		IntBuffer intBufferR = audioBufferR.duplicate();
		if (fadeInFadeOutEnabled) {
			sid.setSampler(new LinearFadingSampleMixer(intBufferL, intBufferR));
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
		boolean mono = sids.size() == 1;
		boolean fakeStereo = isFakeStereo();
		int sidNum = 0;
		for (ReSIDBase sid : sids) {
			SampleMixer sampler = (SampleMixer) sid.getSampler();
			if (mono) {
				sampler.setVolume(volume[sidNum], volume[sidNum]);
				sampler.setDelay(0);
			} else {
				double leftFraction = positionL[sidNum];
				double rightFraction = positionR[sidNum];
				if (!fakeStereo) {
					leftFraction = Math.sqrt(2 * leftFraction);
					rightFraction = Math.sqrt(2 * rightFraction);
				}
				int volumeL = (int) (volume[sidNum] * leftFraction);
				int volumeR = (int) (volume[sidNum] * rightFraction);
				sampler.setVolume(volumeL, volumeR);
				sampler.setDelay(delayInSamples[sidNum]);
			}
			sidNum++;
		}
	}

	/**
	 * @return is fake stereo enabled?
	 */
	private boolean isFakeStereo() {
		return sids.stream().anyMatch(sid -> sid instanceof builder.resid.residfp.ReSIDfp.FakeStereo
				|| sid instanceof builder.resid.resid.ReSID.FakeStereo);
	}

	/**
	 * Doubles speed factor.
	 */
	@Override
	public void fastForward() {
		mixerAudio.fastForwardShift = Math.min(mixerAudio.fastForwardShift + 1, MAX_FAST_FORWARD + VOLUME_SCALER);
		mixerAudio.fastForwardBitMask = (1 << mixerAudio.fastForwardShift - VOLUME_SCALER) - 1;
	}

	/**
	 * Use normal speed factor.
	 */
	@Override
	public void normalSpeed() {
		mixerAudio.fastForwardShift = VOLUME_SCALER;
		mixerAudio.fastForwardBitMask = 0;
	}

	@Override
	public boolean isFastForward() {
		return mixerAudio.fastForwardShift - VOLUME_SCALER != 0;
	}

	@Override
	public int getFastForwardBitMask() {
		return mixerAudio.fastForwardBitMask;
	}

	public WhatsSidSupport getWhatsSidSupport() {
		return whatsSidSupport;
	}

	public void setWhatsSidEnabled(boolean whatsSidEnabled) {
		this.whatsSidEnabled = whatsSidEnabled;
	}

}