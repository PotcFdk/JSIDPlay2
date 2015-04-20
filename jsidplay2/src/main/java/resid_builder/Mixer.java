package resid_builder;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.components.pla.PLA;
import resid_builder.resample.Resampler;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IAudioSection;

/**
 * Mixer to mix SIDs sample data into the audio buffer.<BR>
 * Note: the audibility on the left/right speaker require two audio buffers, one
 * for each channel.
 * 
 * @author ken
 *
 */
public class Mixer {
	/**
	 * Sound sample consumer consuming sample data while a SID is being
	 * clock'ed. A sample value is added to the audio buffer to mix the output
	 * of several SIDs together.
	 * 
	 * @author ken
	 *
	 */
	private class SampleAdder implements IntConsumer {
		/**
		 * @param sidNum
		 *            SID chip number
		 */
		private int sidNum;
		/**
		 * Current audio buffer position.
		 */
		private int pos;

		public SampleAdder(int sidNum) {
			this.sidNum = sidNum;
		}

		/**
		 * Add sample to the mix with respect to the speakers audibility.
		 */
		@Override
		public void accept(int sample) {
			int valueL = audioBufferL.get(pos);
			int valueR = audioBufferR.get(pos);
			valueL += sample * balancedVolumeL[sidNum];
			valueR += sample * balancedVolumeR[sidNum];
			audioBufferL.put(pos, valueL);
			audioBufferR.put(pos++, valueR);
		}

		public void rewind() {
			pos = 0;
		}

		public int getPos() {
			return pos;
		}

	}

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
				SampleAdder sampler = (SampleAdder) sid.getSampler();
				// clock SID to the present moment
				sid.clock();
				// rewind
				sampler.rewind();
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
		 * Random source for triangular dithering
		 */
		private Random RANDOM = new Random();
		/**
		 * State of HP-TPDF.
		 */
		private int oldRandomValue;

		/**
		 * Note: The assumption, that after clocking two chips their buffer
		 * positions are equal is wrong! Using different SID reimplementations
		 * one chip can be one sample further than the other. Therefore we have
		 * to handle overflowing sample data to prevent crackling noises. This
		 * implementation just cuts them off.
		 */
		@Override
		public void event() throws InterruptedException {
			// Clock SIDs to fill audio buffer
			int numSamples = 0;
			for (ReSIDBase sid : sids) {
				SampleAdder sampler = (SampleAdder) sid.getSampler();
				// clock SID to the present moment
				sid.clock();
				// determine amount of samples produced (cut-off overflows)
				numSamples = numSamples != 0 ? Math.min(numSamples,
						sampler.getPos()) : sampler.getPos();
				// rewind
				sampler.rewind();
			}
			// Output sample data
			for (int pos = 0; pos < numSamples; pos++) {
				int dither = triangularDithering();

				putSample(resamplerL, audioBufferL.get(pos), dither);
				putSample(resamplerR, audioBufferR.get(pos), dither);

				if (driver.buffer().remaining() == 0) {
					driver.write();
					driver.buffer().clear();
				}
				audioBufferL.put(pos, 0);
				audioBufferR.put(pos, 0);
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
	 * Balanced left speaker volume = volumeL * positionL.
	 */
	private int[] balancedVolumeL = new int[PLA.MAX_SIDS];
	/**
	 * Balanced right speaker volume = volumeR * positionR.
	 */
	private int[] balancedVolumeR = new int[PLA.MAX_SIDS];

	public Mixer(EventScheduler context, CPUClock cpuClock,
			AudioConfig audioConfig, IAudioSection audioSection,
			AudioDriver audioDriver) {
		this.context = context;
		this.driver = audioDriver;
		this.audioBufferL = IntBuffer.allocate(audioSection.getBufferSize());
		this.audioBufferR = IntBuffer.allocate(audioSection.getBufferSize());
		this.resamplerL = Resampler.createResampler(cpuClock.getCpuFrequency(),
				audioConfig.getSamplingMethod(), audioConfig.getFrameRate(),
				20000);
		this.resamplerR = Resampler.createResampler(cpuClock.getCpuFrequency(),
				audioConfig.getSamplingMethod(), audioConfig.getFrameRate(),
				20000);
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
	 * @param audio
	 *            audio configuration
	 */
	public void add(int sidNum, ReSIDBase sid, IAudioSection audio) {
		sid.setSampler(new SampleAdder(sidNum));
		if (sidNum < sids.size()) {
			sids.set(sidNum, sid);
		} else {
			sids.add(sid);
			setVolume(sidNum, audio);
			setBalance(sidNum, audio);
		}
	}

	/**
	 * Remove SID from the mix.
	 * 
	 * @param sid
	 *            SID to remove
	 */
	public void remove(SIDEmu sid) {
		sids.remove(sid);
		balanceVolume();
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