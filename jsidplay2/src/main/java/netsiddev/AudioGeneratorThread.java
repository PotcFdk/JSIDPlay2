package netsiddev;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

import libsidplay.common.CPUClock;
import libsidplay.common.SIDChip;
import libsidplay.common.SamplingMethod;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;

/**
 * Audio generating thread which communicates with SIDWrite source over a
 * BlockingQueue.
 * 
 * @author Antti Lankila
 */
public class AudioGeneratorThread extends Thread {
	/** Random source for triangular dithering */
	private static final Random RANDOM = new Random();

	/** Current clock in the SID stream. */
	private final AtomicLong playbackClock = new AtomicLong(0);

	/**
	 * Queue with SID writes from client. We reserve a space assuming writes
	 * come at most one every 10 cpu clocks.
	 */
	private final BlockingQueue<SIDWrite> sidCommandQueue;

	/** global setting for each 8580 if digiboost should be enabled */
	private boolean digiBoostEnabled = false;

	/** SIDs that generate output */
	private SIDChip[] sid;

	/** Currently active clocking value */
	private CPUClock[] sidClocking;

	/** Currently active sampling method */
	private SamplingMethod[] sidSampling;

	/** Current audio output frequency. */
	private final AudioConfig audioConfig;

	/** State of HP-TPDF. */
	private int oldRandomValue;

	private int[] sidLevel;

	private int deviceIndex;

	private int[] sidPositionL;

	private int[] sidPositionR;

	private Mixer.Info mixerInfo;
	private boolean deviceChanged = false;

	/** Is audio thread waiting? */
	private final AtomicBoolean audioWait = new AtomicBoolean(true);

	/** Is audio thread requested to stop rapidly? */
	private final AtomicBoolean quicklyDiscardAudio = new AtomicBoolean(false);

	/**
	 * Triangularly shaped noise source for audio applications. Output of this
	 * PRNG is between ]-1, 1[.
	 * 
	 * @return triangular noise sample
	 */
	protected int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x01;
		return oldRandomValue - prevValue;
	}

	public AudioGeneratorThread(AudioConfig config) {
		setPriority(Thread.MAX_PRIORITY);
		sidCommandQueue = new LinkedBlockingQueue<SIDWrite>();
		audioConfig = config;

		SIDDeviceSettings settings = SIDDeviceSettings.getInstance();

		deviceIndex = settings.getDeviceIndex();
		digiBoostEnabled = settings.getDigiBoostEnabled();
	}

	@Override
	public void run() {
		/** Audio output driver. */
		JavaSound driver = new JavaSound();

		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		try {
			if (deviceIndex >= 0 && deviceIndex < aInfos.length) {
				mixerInfo = aInfos[deviceIndex];
				driver.open(audioConfig, mixerInfo);
			} else {
				driver.open(audioConfig, (Info) null);
			}

			/* Do sound 10 ms at a time. */
			final int audioLength = 10000;
			/* Allocate audio buffer about 2x needed. */
			int[] audioBuffer = new int[2 * audioLength
					/ (1000000 / audioConfig.getFrameRate())];
			int[] outAudioBuffer = new int[audioBuffer.length];

			/* Wait for configuration/commands initially. */
			synchronized (sidCommandQueue) {
				sidCommandQueue.wait();
				audioWait.set(false);
			}
			refreshParams();

			while (true) {
				SIDWrite write = sidCommandQueue.poll();

				/* Ran out of writes? */
				if (write == null) {
					long predictedExhaustionTime = System.currentTimeMillis()
							+ driver.getRemainingPlayTime();
					while (!quicklyDiscardAudio.get()
							&& System.currentTimeMillis() < predictedExhaustionTime) {
						/*
						 * Sleep for 1 ms, then re-check quicklyDiscardAudio
						 * flag.
						 */
						write = sidCommandQueue.poll(1, TimeUnit.MILLISECONDS);
						if (write != null) {
							break;
						}
					}
					quicklyDiscardAudio.getAndSet(false);

					/* Okay, no dice; we must stop. */
					if (write == null) {
						synchronized (audioWait) {
							audioWait.set(true);
							audioWait.notify();
						}
						driver.pause();
						synchronized (sidCommandQueue) {
							sidCommandQueue.wait();
							audioWait.set(false);
						}
						continue;
					}
				}

				int cycles = write.getCycles();
				while (cycles != 0) {
					int piece = Math.min(cycles, audioLength);

					/* First SID initializes the buffer. */
					final int audioBufferPos = sid[0].clock(piece, audioBuffer,
							0);
					for (int i = 0; i < audioBufferPos; i++) {
						int sample = audioBuffer[i] * sidLevel[0] >> 10;
						outAudioBuffer[i << 1 | 0] = sample * sidPositionL[0] >> 10;
						outAudioBuffer[i << 1 | 1] = sample * sidPositionR[0] >> 10;
					}

					/* Other SIDs are added to mix. */
					for (int sidNum = 1; sidNum < sid.length; sidNum++) {
						sid[sidNum].clock(piece, audioBuffer, 0);
						for (int i = 0; i < audioBufferPos; i++) {
							int sample = audioBuffer[i] * sidLevel[sidNum] >> 10;
							outAudioBuffer[i << 1 | 0] += sample
									* sidPositionL[sidNum] >> 10;
							outAudioBuffer[i << 1 | 1] += sample
									* sidPositionR[sidNum] >> 10;
						}
					}

					/*
					 * Note: if we need > 2 SIDs, we could consider mixing them
					 * together before resampling, which would allow us to run
					 * the sinc code only twice. Additionally, we might define
					 * stereo sinc resampler to do both passes at once. This
					 * should be a win because the FIR table would only have to
					 * be fetched once.
					 */

					/* Generate triangularly dithered stereo audio output. */
					final ByteBuffer output = driver.buffer();
					for (int i = 0; i < audioBufferPos; i++) {
						int dithering = triangularDithering();
						int value;

						value = outAudioBuffer[i << 1 | 0] + dithering;
						if (value > 32767) {
							value = 32767;
						}
						if (value < -32768) {
							value = -32768;
						}
						output.putShort((short) value);

						value = outAudioBuffer[i << 1 | 1] + dithering;
						if (value > 32767) {
							value = 32767;
						}
						if (value < -32768) {
							value = -32768;
						}
						output.putShort((short) value);

						if (!output.hasRemaining()) {
							driver.write();
							output.clear();
						}
					}

					playbackClock.addAndGet(piece);
					cycles -= piece;
				}

				/* do the write, if this is a write command */
				if (write.isEnd()) {
					/* 0-pad output, write one last time */
					final ByteBuffer output = driver.buffer();
					if (output.position() != 0) {
						while (output.hasRemaining()) {
							output.putShort((short) 0);
							output.putShort((short) 0);
						}
						driver.write();
					}
					break;
				}

				if (!write.isPureDelay()) {
					sid[write.getChip()].write(write.getRegister(),
							write.getValue());
				}

				if (deviceChanged) {
					driver.setAudioDevice(mixerInfo);
					deviceChanged = false;
				}
			}
		} catch (final InterruptedException e) {
		} catch (final LineUnavailableException e) {
			e.printStackTrace();
		} finally {
			driver.close();
		}
	}

	/**
	 * Reset the specified SID and sets the volume afterwards.
	 * 
	 * @param sidNumber
	 *            The specified SID to reset.
	 * @param volume
	 *            The volume of the specified SID after resetting it.
	 */
	public void reset(final int sidNumber, final byte volume) {
		sid[sidNumber].reset();
		sid[sidNumber].write(0x18, volume);
	}

	/**
	 * Mute a SID's voice.
	 * 
	 * @param sidNumber
	 *            The specified SID to mute the voice of.
	 * @param voiceNo
	 *            The specific voice of the SID to mute.
	 * @param mute
	 *            Mute/Unmute the SID voice.
	 */
	public void mute(final int sidNumber, final int voiceNo, final boolean mute) {
		sid[sidNumber].mute(voiceNo, mute);
	}

	/**
	 * Change the output device
	 * 
	 * @param deviceInfo
	 */
	public void changeDevice(final Mixer.Info deviceInfo) {
		mixerInfo = deviceInfo;
		deviceChanged = true;
	}

	/**
	 * Set NTSC/PAL time source.
	 * 
	 * @param clock
	 *            The specified clock value to set.
	 */
	public void setClocking(CPUClock clock) {
		Arrays.fill(sidClocking, clock);
		refreshParams();
	}

	/**
	 * Set quality of audio output.
	 * 
	 * @param samplingMethod
	 *            The desired sampling method to use.
	 */
	public void setSampling(SamplingMethod samplingMethod) {
		Arrays.fill(sidSampling, samplingMethod);
		refreshParams();
	}

	/**
	 * Update SID parameters to new settings based on given clocking, sampling
	 * and output frequency.
	 */
	private void refreshParams() {
		for (int i = 0; i < sid.length; i++) {
			sid[i].setSamplingParameters(sidClocking[i].getCpuFrequency(),
					sidSampling[i], audioConfig.getFrameRate(), 20000);
		}
	}

	public void setPosition(int sidNumber, int position) {
		if (sid.length > 1) {
			float rightFraction = (position + 100) / 200f;
			float leftFraction = 1f - rightFraction;
			float power = (float) Math.sqrt(leftFraction * leftFraction
					+ rightFraction * rightFraction);
			sidPositionL[sidNumber] = (int) (1024 * leftFraction / power);
			sidPositionR[sidNumber] = (int) (1024 * rightFraction / power);
		} else {
			sidPositionL[sidNumber] = 1024;
			sidPositionR[sidNumber] = 1024;
		}
	}

	public void setLevelAdjustment(int sid, int level) {
		sidLevel[sid] = (int) (1024 * Math.pow(10.0, level / 100.0));
	}

	/**
	 * Acquire command queue handle.
	 * 
	 * @return command queue
	 */
	public BlockingQueue<SIDWrite> getSidCommandQueue() {
		return sidCommandQueue;
	}

	/**
	 * Return the current clock in the SID stream.
	 *
	 * @return the clock
	 */
	public long getPlaybackClock() {
		return playbackClock.get();
	}

	/**
	 * Ensure that the event-handling thread is consuming events.
	 */
	public void ensureDraining() {
		synchronized (sidCommandQueue) {
			sidCommandQueue.notify();
		}
	}

	public void ensureQuickDraining() {
		synchronized (sidCommandQueue) {
			quicklyDiscardAudio.getAndSet(true);
			sidCommandQueue.notify();
		}
	}

	public boolean isWaitingForCommands() {
		return audioWait.get();
	}

	public boolean waitUntilQueueReady(long timeout) {
		boolean isQueueReady = audioWait.get();
		if (!isQueueReady) {
			ensureQuickDraining();
			try {
				synchronized (audioWait) {
					audioWait.wait(timeout);
				}
			} catch (InterruptedException e) {
			}
			isQueueReady = audioWait.get();
		}
		return isQueueReady;
	}

	public void setSidArray(SIDChip[] sid) {
		this.sid = sid;

		sidClocking = new CPUClock[sid.length];
		Arrays.fill(sidClocking, CPUClock.PAL);

		sidSampling = new SamplingMethod[sid.length];
		Arrays.fill(sidSampling, SamplingMethod.DECIMATE);

		sidLevel = new int[sid.length];
		sidPositionL = new int[sid.length];
		sidPositionR = new int[sid.length];

		for (int i = 0; i < sid.length; i++) {
			setLevelAdjustment(i, 0);
			if (sid.length > 1) {
				setPosition(i, -100 + 200 * i / (sid.length - 1));
			} else {
				setPosition(i, 0);
			}
		}
	}

	public void setSID(int sidNumber, SIDChip sidConfig) {
		sid[sidNumber] = sidConfig;
		setDigiBoost(digiBoostEnabled);
	}

	/**
	 * Whether or not to enable Digiboost for all SID chips of model 8580.
	 *
	 * @param selected
	 *            Whether or not to enable Digiboost.
	 */
	public void setDigiBoost(final boolean selected) {
		digiBoostEnabled = selected;

		for (SIDChip sidChip : sid) {
			if (digiBoostEnabled && sidChip != null) {
				sidChip.input(sidChip.getInputDigiBoost());
			}
		}
	}
}