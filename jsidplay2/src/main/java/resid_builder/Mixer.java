package resid_builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import sidplay.audio.AudioConfig;
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
				sid.bufferpos = 0;
			}
			context.schedule(nullAudio, 10000);
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
		 * positions are equal is false! Under some circumstance one chip can be
		 * one sample further than the other. Therefore we have to handle
		 * overflowing sample data to prevent crackling noises. This
		 * implementation just cuts them off.
		 */
		@Override
		public synchronized void event() throws InterruptedException {
			int samples = -1;
			for (ReSIDBase sid : sids) {
				// clock SID to the present moment
				sid.clock();
				// determine amount of samples produced (cut-off overflows)
				samples = samples != -1 ? Math.min(samples, sid.bufferpos)
						: sid.bufferpos;
				sid.bufferpos = 0;
			}
			// output sample data
			for (int sampleIdx = 0; sampleIdx < samples; sampleIdx++) {
				int dither = triangularDithering();

				putSample(sampleIdx, channels > 1 ? balancedVolumeL : volume,
						dither);
				if (channels > 1) {
					putSample(sampleIdx, balancedVolumeR, dither);
				}
				if (driver.buffer().remaining() == 0) {
					driver.write();
					driver.buffer().clear();
				}
			}
			context.schedule(this, 10000);
		}
	}

	/**
	 * System event context.
	 */
	private EventScheduler context;

	private Event nullAudio = new NullAudioEvent("NullAudio");
	private Event mixerAudio = new MixerEvent("MixerAudio");

	/**
	 * SIDs to mix their sound output.
	 */
	private List<ReSIDBase> sids = new ArrayList<ReSIDBase>();
	/**
	 * Channels used (1=Mono, 2=Stereo).
	 */
	private int channels;
	/**
	 * Audio driver
	 */
	private AudioDriver driver;

	/**
	 * Random source for triangular dithering
	 */
	private final Random RANDOM = new Random();
	/**
	 * State of HP-TPDF.
	 */
	private int oldRandomValue;

	/**
	 * Volume of all SIDs.
	 */
	private final float[] volume = new float[] { 1024f, 1024f, 1024f };
	/**
	 * SID audibility on the left speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionL = new float[] { 1, 0, .5f };
	/**
	 * SID audibility on the right speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionR = new float[] { 0, 1, .5f };

	/**
	 * Balanced left speaker volume = volumeL * positionL.
	 */
	private final float[] balancedVolumeL = new float[] { 1024, 0, 512 };
	/**
	 * Balanced right speaker volume = volumeR * positionR.
	 */
	private final float[] balancedVolumeR = new float[] { 0, 1024, 512 };

	public Mixer(EventScheduler context, AudioDriver audioDriver) {
		this.context = context;
		this.driver = audioDriver;
	}

	public void reset() {
		context.cancel(nullAudio);
		context.schedule(nullAudio, 0, Event.Phase.PHI2);
	}

	/**
	 * Starts mixing the outputs of several SIDs. Write samples to the sound
	 * buffer.
	 */
	public synchronized void start(AudioConfig audioConfig,
			IAudioSection audioSection) {
		context.cancel(nullAudio);
		context.cancel(mixerAudio);
		this.channels = audioConfig.getChannels();
		for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
			setVolume(sidNum, audioSection);
			setBalance(sidNum, audioSection);
		}
		context.schedule(mixerAudio, 0, Event.Phase.PHI2);
	}

	public synchronized void add(int sidNum, ReSIDBase sid) {
		if (sidNum < sids.size()) {
			sids.set(sidNum, sid);
		} else {
			sids.add(sid);
		}
	}

	public synchronized void remove(SIDEmu sid) {
		sids.remove(sid);
	}

	public synchronized ReSIDBase get(int sidNum) {
		return sids.get(sidNum);
	}

	/**
	 * @return current number of devices.
	 */
	public synchronized int getNumDevices() {
		return sids.size();
	}

	/**
	 * Triangularly shaped noise source for audio applications. Output of this
	 * PRNG is between ]-1, 1[ * 1024.
	 * 
	 * @return triangular noise sample
	 */
	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x3ff;
		return oldRandomValue - prevValue;
	}

	private final void putSample(int sampleIdx, float[] balancedVolume,
			int dither) {
		int value = 0;
		int sidNum = 0;
		for (ReSIDBase sid : sids) {
			value += sid.buffer[sampleIdx] * balancedVolume[sidNum++];
		}
		value = value + dither >> 10;

		if (value > 32767) {
			value = 32767;
		}
		if (value < -32768) {
			value = -32768;
		}
		driver.buffer().putShort((short) value);
	}

	/**
	 * Volume of the SID chip.<BR>
	 * 0(-6db)..12(+6db)
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param audio
	 *            audio configuration
	 */
	public void setVolume(int sidNum, IAudioSection audio) {
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
		volume[sidNum] = (float) Math.pow(10, volumeInDB / 10) * 1024;
		updateFactor();
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
		positionL[sidNum] = 1 - balance;
		positionR[sidNum] = balance;
		updateFactor();
	}

	private void updateFactor() {
		for (int i = 0; i < balancedVolumeL.length; i++) {
			balancedVolumeL[i] = volume[i] * positionL[i];
			balancedVolumeR[i] = volume[i] * positionR[i];
		}
	}

}