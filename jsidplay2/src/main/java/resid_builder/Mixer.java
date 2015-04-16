package resid_builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.common.SamplingMethod;
import libsidplay.components.pla.PLA;
import resid_builder.resample.Resampler;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IAudioSection;

public class Mixer {
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
				// clock SID to the present moment
				sid.clock();
				// rewind buffer
				sid.bufferpos = 0;
			}
			context.schedule(this, 10000);
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
		 * Note: The assumption, that after clocking two chips their buffer
		 * positions are equal is false! Using different SID reimplementations
		 * one chip can be one sample further than the other. Therefore we have
		 * to handle overflowing sample data to prevent crackling noises. This
		 * implementation just cuts them off.
		 */
		@Override
		public void event() throws InterruptedException {
			// clock all SIDs and buffer sample data
			int numSamples = 0;
			for (ReSIDBase sid : sids) {
				// clock SID to the present moment
				sid.clock();
				// determine amount of samples produced (cut-off overflows)
				numSamples = numSamples != 0 ? Math.min(numSamples,
						sid.bufferpos) : sid.bufferpos;
				// rewind buffer
				sid.bufferpos = 0;
			}
			// For all sample data
			for (int sampleIdx = 0; sampleIdx < numSamples; sampleIdx++) {
				// Mix all SIDs sample with respect to the speakers audibility
				int sidNum = 0;
				int valueL = 0, valueR = 0;
				for (ReSIDBase sid : sids) {
					int sample = sid.buffer[sampleIdx];
					valueL += sample * balancedVolumeL[sidNum];
					valueR += sample * balancedVolumeR[sidNum++];
				}
				// output sample data
				int dither = triangularDithering();
				putSample(resamplerL, valueL >> 10, dither);
				putSample(resamplerR, valueR >> 10, dither);

				if (driver.buffer().remaining() == 0) {
					driver.write();
					driver.buffer().clear();
				}
			}
			context.schedule(this, 10000);
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
			if (resampler.input(value)) {
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

	}

	/**
	 * System event context.
	 */
	private EventScheduler context;

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
	 * Audio driver
	 */
	private AudioDriver driver;

	/**
	 * Random source for triangular dithering
	 */
	private Random RANDOM = new Random();
	/**
	 * State of HP-TPDF.
	 */
	private int oldRandomValue;

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
	 * Balanced left speaker volume = volumeL * positionL.
	 */
	private int[] balancedVolumeL = new int[PLA.MAX_SIDS];
	/**
	 * Balanced right speaker volume = volumeR * positionR.
	 */
	private int[] balancedVolumeR = new int[PLA.MAX_SIDS];

	/**
	 * Resampler of sample output for two channels (stereo).
	 */
	private Resampler resamplerL, resamplerR;

	public Mixer(EventScheduler context, AudioDriver audioDriver) {
		this.context = context;
		this.driver = audioDriver;
	}

	public void reset() {
		context.schedule(nullAudio, 0, Event.Phase.PHI2);
	}

	public void setSampling(final double systemClock, final float freq,
			final SamplingMethod method, double highestAccurateFrequency) {
		resamplerL = Resampler.createResampler(systemClock, method, freq,
				highestAccurateFrequency);
		resamplerR = Resampler.createResampler(systemClock, method, freq,
				highestAccurateFrequency);
	}

	/**
	 * Starts mixing the outputs of several SIDs. Write samples to the sound
	 * buffer.
	 */
	public void start() {
		context.cancel(nullAudio);
		context.schedule(mixerAudio, 0, Event.Phase.PHI2);
	}

	public void add(int sidNum, ReSIDBase sid, IAudioSection audio) {
		if (sidNum < sids.size()) {
			sids.set(sidNum, sid);
		} else {
			setVolume(sidNum, audio);
			setBalance(sidNum, audio);
			sids.add(sid);
			balanceVolume();
		}
	}

	public void remove(SIDEmu sid) {
		sids.remove(sid);
		balanceVolume();
	}

	public ReSIDBase get(int sidNum) {
		return sids.get(sidNum);
	}

	/**
	 * @return current number of devices.
	 */
	public int getNumDevices() {
		return sids.size();
	}

	/**
	 * Triangularly shaped noise source for audio applications. Output of this
	 * PRNG is between ]-1, 1[.
	 * 
	 * @return triangular noise sample
	 */
	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x1;
		return oldRandomValue - prevValue;
	}

	/**
	 * Volume of the SID chip.<BR>
	 * -6(-6db)..6(+6db)
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            audio configuration
	 */
	public void setVolume(int sidNum, IAudioSection audio) {
		assert sidNum < sids.size();

		float volumeInDB;
		switch (sidNum) {
		case 0:
			volumeInDB = audio.getMainVolume();
			break;
		case 1:
			volumeInDB = audio.getSecondVolume();
			break;
		case 2:
			volumeInDB = audio.getThirdVolume();
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		assert volumeInDB >= -6 && volumeInDB <= 6;
		volume[sidNum] = (int) (Math.pow(10, volumeInDB / 10) * 1024);
		balanceVolume();
	}

	/**
	 * Set left/right speaker balance for each SID.<BR>
	 * 0(left speaker)..0.5(centered)..1(right speaker)
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            audio configuration
	 */
	public void setBalance(int sidNum, IAudioSection audio) {
		assert sidNum < sids.size();

		float balance;
		switch (sidNum) {
		case 0:
			balance = audio.getMainBalance();
			break;
		case 1:
			balance = audio.getSecondBalance();
			break;
		case 2:
			balance = audio.getThirdBalance();
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		assert balance >= 0 && balance <= 1;
		positionL[sidNum] = 1 - balance;
		positionR[sidNum] = balance;
		balanceVolume();
	}

	/**
	 * Calculate balanced speaker volume level.<BR>
	 * Mono output: Use volume.<BR>
	 * Stereo or 3-SID output: Use speaker audibility and volume.
	 */
	private void balanceVolume() {
		boolean mono = sids.size() < 2;
		for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
			if (mono) {
				balancedVolumeL[sidNum] = volume[sidNum];
				balancedVolumeR[sidNum] = volume[sidNum];
			} else {
				balancedVolumeL[sidNum] = (int) (volume[sidNum] * positionL[sidNum]);
				balancedVolumeR[sidNum] = (int) (volume[sidNum] * positionR[sidNum]);
			}
		}
	}
}