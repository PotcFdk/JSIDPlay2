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
		 * positions are equal is false! Under some circumstance one chip can be
		 * one sample further than the other. Therefore we have to handle
		 * overflowing sample data to prevent crackling noises. This
		 * implementation just cuts them off.
		 */
		@Override
		public void event() throws InterruptedException {
			int samples = -1;
			for (ReSIDBase sid : sids) {
				// clock SID to the present moment
				sid.clock();
				// determine amount of samples produced (cut-off overflows)
				samples = samples != -1 ? Math.min(samples, sid.bufferpos)
						: sid.bufferpos;
				sid.bufferpos = 0;
			}
			boolean hasMoreSids = sids.size() > 1;
			// output sample data
			for (int sampleIdx = 0; sampleIdx < samples; sampleIdx++) {
				int dither = triangularDithering();

				putSample(sampleIdx, hasMoreSids ? balancedVolumeL : volume,
						dither);
				putSample(sampleIdx, hasMoreSids ? balancedVolumeR : volume,
						dither);
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
	private final int[] volume = new int[3];
	/**
	 * SID audibility on the left speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionL = new float[3];
	/**
	 * SID audibility on the right speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionR = new float[3];

	/**
	 * Balanced left speaker volume = volumeL * positionL.
	 */
	private final int[] balancedVolumeL = new int[3];
	/**
	 * Balanced right speaker volume = volumeR * positionR.
	 */
	private final int[] balancedVolumeR = new int[3];

	public Mixer(EventScheduler context, AudioDriver audioDriver) {
		this.context = context;
		this.driver = audioDriver;
	}

	public void reset() {
		context.schedule(nullAudio, 0, Event.Phase.PHI2);
	}

	/**
	 * Starts mixing the outputs of several SIDs. Write samples to the sound
	 * buffer.
	 */
	public void start(AudioConfig audioConfig, IAudioSection audioSection) {
		for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
			setVolume(sidNum, audioSection);
			setBalance(sidNum, audioSection);
		}
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
		}
	}

	public void remove(SIDEmu sid) {
		sids.remove(sid);
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
	 * PRNG is between ]-1, 1[ * 1024.
	 * 
	 * @return triangular noise sample
	 */
	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x3ff;
		return oldRandomValue - prevValue;
	}

	private final void putSample(int sampleIdx, int[] balancedVolume, int dither) {
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
	 * -6(-6db)..6(+6db)
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
		assert volumeInDB >= -6 && volumeInDB <= 6;
		volume[sidNum] = (int) (Math.pow(10, volumeInDB / 10) * 1024);
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
		assert balance >= 0 && balance <= 1;
		positionL[sidNum] = 1 - balance;
		positionR[sidNum] = balance;
		updateFactor();
	}

	private void updateFactor() {
		for (int i = 0; i < balancedVolumeL.length; i++) {
			balancedVolumeL[i] = (int) (volume[i] * positionL[i]);
			balancedVolumeR[i] = (int) (volume[i] * positionR[i]);
		}
	}

}