package libsidplay.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.sidtune.SidTune;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;

public abstract class ReSIDBuilderBase implements SIDBuilder {
	public class MixerEvent extends Event {
		/** Random source for triangular dithering */
		private final Random RANDOM = new Random();
		/** State of HP-TPDF. */
		private int oldRandomValue;

		private final int[] volume = new int[] { 1024, 1024, 1024 };
		private final float[] positionL = new float[] { 1, 0, .5f };
		private final float[] positionR = new float[] { 0, 1, .5f };

		private transient final float[] balancedVolumeL = new float[] { 1024,
				0, 512 };
		private transient final float[] balancedVolumeR = new float[] { 0,
				1024, 512 };

		private void setVolume(int i, float v) {
			this.volume[i] = (int) (v * 1024);
			updateFactor();
		}

		private void setBalance(int i, float balance) {
			this.positionL[i] = 1 - balance;
			this.positionR[i] = balance;
			updateFactor();
		}

		private void updateFactor() {
			for (int i = 0; i < balancedVolumeL.length; i++) {
				balancedVolumeL[i] = volume[i] * positionL[i];
				balancedVolumeR[i] = volume[i] * positionR[i];
			}
		}

		/**
		 * Triangularly shaped noise source for audio applications. Output of
		 * this PRNG is between ]-1, 1[ * 1024.
		 * 
		 * @return triangular noise sample
		 */
		private int triangularDithering() {
			int prevValue = oldRandomValue;
			oldRandomValue = RANDOM.nextInt() & 0x3ff;
			return oldRandomValue - prevValue;
		}

		protected MixerEvent() {
			super("Mixer");
		}

		@Override
		public void event() throws InterruptedException {
			int samples = 0;
			int numBuffers = 0;
			for (SIDEmu sid : sids) {
				/*
				 * Clocks the SID to the present moment, if it isn't already.
				 */
				sid.clock();
				buffers[numBuffers++] = sid.buffer;
				/*
				 * extract buffer info now that the SID is updated. If chip2
				 * exists, its bufferpos is expected to be identical to chip1's.
				 */
				samples = samples == 0 ? sid.bufferpos : samples;
				sid.bufferpos = 0;
			}
			for (int sampleIdx = 0; sampleIdx < samples; sampleIdx++) {
				int dither = triangularDithering();

				putSample(numBuffers, sampleIdx, balancedVolumeL, dither);
				if (channels > 1) {
					putSample(numBuffers, sampleIdx, balancedVolumeR, dither);
				}
				if (soundBuffer.remaining() == 0) {
					driver.write();
					soundBuffer.clear();
				}
			}
			context.schedule(this, 10000);
		}

		private final void putSample(int numBuffers, int sampleIdx,
				float[] balancedVolume, int dither) {
			int value = 0;
			for (int bufferIdx = 0; bufferIdx < numBuffers; bufferIdx++) {
				value += buffers[bufferIdx][sampleIdx]
						* balancedVolume[bufferIdx];
			}
			value = value + dither >> 10;

			if (value > 32767) {
				value = 32767;
			}
			if (value < -32768) {
				value = -32768;
			}
			soundBuffer.putShort((short) value);
		}

	}

	/** Current audio configuration */
	private final AudioConfig audioConfig;

	/** C64 system frequency */
	private final CPUClock cpuClock;

	private int[][] buffers;
	private int channels;
	private ByteBuffer soundBuffer;

	/** output driver */
	protected AudioDriver driver, realDriver;

	/** List of SID instances */
	protected List<SIDEmu> sids = new ArrayList<SIDEmu>();

	/** Mixing algorithm */
	protected final MixerEvent mixerEvent = new MixerEvent();

	private EventScheduler context;

	public ReSIDBuilderBase(IConfig config, AudioConfig audioConfig,
			CPUClock cpuClock, AudioDriver audio, SidTune tune) {
		this.audioConfig = audioConfig;
		this.cpuClock = cpuClock;
		this.driver = audio;
		switchToNullDriver(tune);
	}

	public void start(final EventScheduler context) {
		this.context = context;
		/*
		 * No matter how many chips are in use, mixerEvent is singleton with
		 * respect to them. Only one will be scheduled. This is a bit dirty,
		 * though.
		 */
		context.cancel(mixerEvent);
		buffers = new int[sids.size()][];
		channels = audioConfig.getChannels();
		soundBuffer = realDriver.buffer();
		context.schedule(mixerEvent, 0, Event.Phase.PHI2);
		switchToAudioDriver();
	}

	public int getNumDevices() {
		return sids.size();
	}

	public SIDEmu lock(final EventScheduler evt, SIDEmu device, ChipModel model) {
		if (device == null) {
			device = lock(evt, model);
		} else {
			device.setChipModel(model);
		}
		return device;
	}

	/**
	 * Make a new SID of right type
	 */
	private SIDEmu lock(final EventScheduler env, final ChipModel model) {
		final SIDEmu sid = createSIDEmu(env);
		sid.setChipModel(model);
		sid.setSampling(cpuClock.getCpuFrequency(), audioConfig.getFrameRate(),
				audioConfig.getSamplingMethod());
		sids.add(sid);
		return sid;
	}

	/**
	 * No implementation, just builder API compat.
	 */
	public void unlock(final SIDEmu sid) {
		sids.remove(sid);
	}

	public void setVolume(int num, IAudioSection audio) {
		float volumeInDB;
		switch (num) {
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
		mixerEvent.setVolume(num, (float) Math.pow(10, volumeInDB / 10));
	}

	public void setBalance(int num, IAudioSection audio) {
		float balance;
		switch (num) {
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
		mixerEvent.setBalance(num, balance);
	}

	/**
	 * Before the timer start time is being reached, use NULL driver to shorten
	 * the duration to wait for the user.
	 * 
	 * @param tune
	 */
	private void switchToNullDriver(SidTune tune) {
		this.realDriver = driver;
		this.driver = Audio.NONE.getAudioDriver();
		try {
			Audio.NONE.getAudioDriver().open(audioConfig, tune);
		} catch (LineUnavailableException | UnsupportedAudioFileException
				| IOException e) {
		}
	}

	/**
	 * When the start time is being reached, switch to the real audio output.
	 */
	private void switchToAudioDriver() {
		driver = realDriver;
	}

	protected abstract SIDEmu createSIDEmu(EventScheduler env);

}
