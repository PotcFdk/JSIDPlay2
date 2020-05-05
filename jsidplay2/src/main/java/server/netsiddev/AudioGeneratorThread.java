package server.netsiddev;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import builder.resid.resample.Resampler;
import libsidplay.common.CPUClock;
import libsidplay.common.SIDChip;
import libsidplay.common.SamplingMethod;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;
import sidplay.fingerprinting.WhatsSidSupport;

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
	 * Queue with SID writes from client. We reserve a space assuming writes come at
	 * most one every 10 cpu clocks.
	 */
	private final BlockingQueue<SIDWrite> sidCommandQueue;

	/** global setting for each 8580 if digiboost should be enabled */
	private boolean digiBoostEnabled = false;

	/** SIDs that generate output */
	private SIDChip[] sids;

	/** SID resampler */
	private Resampler resamplerL, resamplerR;

	/** Currently active clocking value */
	private CPUClock sidClocking;

	/** Currently active sampling method */
	private SamplingMethod sidSampling;

	/** Current audio output frequency. */
	private final AudioConfig audioConfig;

	/** State of HP-TPDF. */
	private int oldRandomValue;

	private int[] sidLevel;

	private int deviceIndex;

	private int[] sidPositionL;

	private int[] sidPositionR;

	private int[] audioBufferPos;

	private IntBuffer[] delayedSamples;

	/**
	 * Fade-in/fade-out time in clock ticks.
	 */
	private long[] fadeInClocks, fadeOutClocks;

	/**
	 * Fade-in/fade-out clock steps until next volume change and current fade-in and
	 * fade-out counters.
	 */
	private long[] fadeInStep, fadeOutStep, fadeInVal, fadeOutVal;

	private Mixer.Info mixerInfo;
	private boolean deviceChanged = false;

	/** Is audio thread waiting? */
	private final AtomicBoolean audioWait = new AtomicBoolean(true);

	/** Is audio thread requested to stop rapidly? */
	private final AtomicBoolean quicklyDiscardAudio = new AtomicBoolean(false);

	/** Audio output driver. */
	private JavaSound driver = new JavaSound();

	/** WhatsSID capture time in seconds */
	private int captureTime;
	
	/** WhatsSID enabled? */
	private boolean whatsSidEnabled;
	
	/** WhatsSID minimum confidence to match */
	private double minimumRelativeConfidence;
	
	/** WhatsSID */
	private WhatsSidSupport whatsSidSupport;
	
	/**
	 * Triangularly shaped noise source for audio applications. Output of this PRNG
	 * is between ]-1, 1[.
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
		audioConfig.setAudioBufferSize(settings.getAudioBufferSize());
		audioConfig.setBufferFrames(settings.getAudioBufferSize());
		captureTime = settings.getWhatsSidCaptureTime();
		whatsSidEnabled = settings.isWhatsSidEnable();
		minimumRelativeConfidence = settings.getWhatsSidMinimumRelativeConfidence();
	}

	@Override
	public void run() {
		try {
			Vector<AudioDevice> audioDevices = new Vector<>();
			AudioDeviceCompare cmp = new AudioDeviceCompare();
			mixerInfo = null;
			int theDeviceIndex = 0;
			for (Info info : AudioSystem.getMixerInfo()) {
				Mixer mixer = AudioSystem.getMixer(info);
				Line.Info lineInfo = new Line.Info(SourceDataLine.class);
				if (mixer.isLineSupported(lineInfo)) {
					AudioDevice audioDeviceItem = new AudioDevice(theDeviceIndex, info);
					audioDevices.add(audioDeviceItem);
					if (theDeviceIndex == 0) {
						// first device name is the primary device driver which can
						// be translated on some systems
						cmp.setPrimaryDeviceName(info.getName());
					}
					if (audioDeviceItem.getIndex() == deviceIndex) {
						this.mixerInfo = audioDeviceItem.getInfo();
					}
				}
				theDeviceIndex++;
			}
			Collections.sort(audioDevices, cmp);
			driver.open(audioConfig, mixerInfo);

			/* Do sound 10 ms at a time. */
			final int audioLength = 10000;
			/* Allocate audio buffer for two channels (stereo) */
			int[] outAudioBuffer = new int[audioLength * 2];

			/* Wait for configuration/commands initially. */
			synchronized (sidCommandQueue) {
				sidCommandQueue.wait();
				audioWait.set(false);
			}
			refreshParams();

			while (!interrupted()) {
				SIDWrite write = sidCommandQueue.poll();

				/* Ran out of writes? */
				if (write == null) {
					long predictedExhaustionTime = System.currentTimeMillis() + driver.getRemainingPlayTime();
					while (!quicklyDiscardAudio.get() && System.currentTimeMillis() < predictedExhaustionTime) {
						/*
						 * Sleep for 1 ms, then re-check quicklyDiscardAudio flag.
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

					/* Mix SID buffers. */
					for (int sidNum = 0; sidNum < sids.length; sidNum++) {
						final int sid = sidNum;
						audioBufferPos[sid] = 0;
						sids[sidNum].clock(piece, sample -> {
							synchronized (this.delayedSamples) {
								IntBuffer delayedSamples = this.delayedSamples[sid];
								if (!delayedSamples.put(sample).hasRemaining()) {
									((Buffer) delayedSamples).flip();
								}
								sample = delayedSamples.get(delayedSamples.position());
							}
							synchronized (fadeInClocks) {
								if (fadeInClocks[sid] > 0) {
									fadeInClocks[sid]--;
									if (--fadeInVal[sid] == 0) {
										fadeInVal[sid] = fadeInStep[sid];
										sidLevel[sid]++;
									}
								} else if (fadeOutClocks[sid] > 0) {
									fadeOutClocks[sid]--;
									if (--fadeOutVal[sid] == 0) {
										fadeOutVal[sid] = fadeOutStep[sid];
										sidLevel[sid]--;
									}
								}
							}
							sample = sample * sidLevel[sid] >> 10;
							outAudioBuffer[audioBufferPos[sid] << 1 | 0] += sample * sidPositionL[sid] >> 10;
							outAudioBuffer[audioBufferPos[sid]++ << 1 | 1] += sample * sidPositionR[sid] >> 10;
						});
					}

					/*
					 * XXX Note: We might define stereo sinc resampler to do both passes at once.
					 * This should be a win because the FIR table would only have to be fetched
					 * once.
					 */

					/* Generate triangularly dithered stereo audio output. */
					final ByteBuffer output = driver.buffer();
					for (int i = 0; i < piece; i++) {
						int dithering = triangularDithering();
						int value;

						value = outAudioBuffer[i << 1 | 0];
						if (resamplerL.input(value)) {
							value = resamplerL.output() + dithering;

							if (value > 32767) {
								value = 32767;
							}
							if (value < -32768) {
								value = -32768;
							}
							output.putShort((short) value);
						}
						value = outAudioBuffer[i << 1 | 1];
						if (resamplerR.input(value)) {
							value = resamplerR.output() + dithering;

							if (value > 32767) {
								value = 32767;
							}
							if (value < -32768) {
								value = -32768;
							}
							output.putShort((short) value);
						}
						if (whatsSidEnabled) {
							whatsSidSupport.output(outAudioBuffer[i << 1 | 0], outAudioBuffer[i << 1 | 1]);
						}
						outAudioBuffer[i << 1 | 0] = 0;
						outAudioBuffer[i << 1 | 1] = 0;

						if (!output.hasRemaining()) {
							driver.write();
							((Buffer) output).clear();
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
					sids[write.getChip()].write(write.getRegister(), write.getValue());
				}

				if (deviceChanged) {
					driver.setAudioDevice(mixerInfo);
					deviceChanged = false;
				}
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final IOException | LineUnavailableException e) {
			e.printStackTrace();
		} finally {
			driver.close();
		}
	}

	/**
	 * Reset the specified SID and sets the volume afterwards.
	 *
	 * @param sidNumber The specified SID to reset.
	 * @param volume    The volume of the specified SID after resetting it.
	 */
	public void reset(final int sidNumber, final byte volume) {
		sids[sidNumber].reset();
		sids[sidNumber].write(0x18, volume);
	}
	
	public void reopen() {
		// Fix for Linux ALSA audio systems, only
		deviceChanged = true;
	}

	/**
	 * Mute a SID's voice.
	 *
	 * @param sidNumber The specified SID to mute the voice of.
	 * @param voiceNo   The specific voice of the SID to mute.
	 * @param mute      Mute/Unmute the SID voice.
	 */
	public void mute(final int sidNumber, final int voiceNo, final boolean mute) {
		sids[sidNumber].mute(voiceNo, mute);
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

	public void setAudioBufferSize(Integer audioBufferSize) {
		audioConfig.setAudioBufferSize(audioBufferSize);
		audioConfig.setBufferFrames(audioBufferSize);
		deviceChanged = true;
	}

	/**
	 * Set NTSC/PAL time source.
	 *
	 * @param clock The specified clock value to set.
	 */
	public void setClocking(CPUClock clock) {
		sidClocking = clock;
		refreshParams();
	}

	/**
	 * Set quality of audio output.
	 *
	 * @param samplingMethod The desired sampling method to use.
	 */
	public void setSampling(SamplingMethod samplingMethod) {
		sidSampling = samplingMethod;
		refreshParams();
	}

	/**
	 * Update SID parameters to new settings based on given clocking, sampling and
	 * output frequency.
	 */
	private void refreshParams() {
		for (int i = 0; i < sids.length; i++) {
			sids[i].setClockFrequency(sidClocking.getCpuFrequency());
		}
		resamplerL = Resampler.createResampler(sidClocking.getCpuFrequency(), sidSampling, audioConfig.getFrameRate(),
				20000);
		resamplerR = Resampler.createResampler(sidClocking.getCpuFrequency(), sidSampling, audioConfig.getFrameRate(),
				20000);
	}

	public void setPosition(int sidNumber, int position) {
		if (sids.length > 1) {
			float leftFraction = (position <= 0) ? 1 : (100 - position) / 100f;
			float rightFraction = (position >= 0) ? 1 : (100 + position) / 100f;
			sidPositionL[sidNumber] = (int) (1024 * leftFraction);
			sidPositionR[sidNumber] = (int) (1024 * rightFraction);
		} else {
			sidPositionL[sidNumber] = 1024;
			sidPositionR[sidNumber] = 1024;
		}
	}

	public void setLevelAdjustment(int sid, int level) {
		sidLevel[sid] = (int) (1024 * Math.pow(10.0, level / 100.0));
	}

	public void setDelay(int sid, int delay) {
		synchronized (this.delayedSamples) {
			int delayedSamples = (int) (sidClocking.getCpuFrequency() / 1000. * delay);
			this.delayedSamples[sid] = (IntBuffer) ByteBuffer.allocateDirect(Integer.BYTES * (delayedSamples + 1))
					.order(ByteOrder.nativeOrder()).asIntBuffer().put(new int[(delayedSamples + 1)]);
			((Buffer) this.delayedSamples[sid]).flip();
		}
	}

	public void setFadeIn(float fadeIn) {
		synchronized (fadeInClocks) {
			for (int sid = 0; sid < sids.length; sid++) {
				this.fadeInClocks[sid] = (long) (fadeIn * sidClocking.getCpuFrequency());
				fadeInVal[sid] = fadeInStep[sid] = sidLevel[sid] != 0 ? fadeInClocks[sid] / sidLevel[sid] : 0;
				sidLevel[sid] = 0;
			}
		}
	}

	public void setFadeOut(float fadeOut) {
		synchronized (fadeInClocks) {
			for (int sid = 0; sid < sids.length; sid++) {
				this.fadeOutClocks[sid] = (long) (fadeOut * sidClocking.getCpuFrequency());
				fadeOutVal[sid] = fadeOutStep[sid] = sidLevel[sid] != 0 ? fadeOutClocks[sid] / sidLevel[sid] : 0;
			}
		}
	}

	/**
	 * Acquire command queue handle.
	 *
	 * @return command queue
	 */
	public BlockingQueue<SIDWrite> getSidCommandQueue() {
		return sidCommandQueue;
	}

	public JavaSound getDriver() {
		return driver;
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
		this.sids = sid;

		sidClocking = CPUClock.PAL;

		sidSampling = SamplingMethod.DECIMATE;

		sidLevel = new int[sid.length];
		sidPositionL = new int[sid.length];
		sidPositionR = new int[sid.length];
		delayedSamples = new IntBuffer[sid.length];
		fadeInClocks = new long[sid.length];
		fadeOutClocks = new long[sid.length];
		fadeInStep = new long[sid.length];
		fadeOutStep = new long[sid.length];
		fadeInVal = new long[sid.length];
		fadeOutVal = new long[sid.length];

		audioBufferPos = new int[sid.length];

		for (int i = 0; i < sid.length; i++) {
			setLevelAdjustment(i, 0);
			if (sid.length > 1) {
				setPosition(i, -100 + 200 * i / (sid.length - 1));
			} else {
				setPosition(i, 0);
			}
			setDelay(i, 0);
		}
		whatsSidSupport = new WhatsSidSupport(sidClocking.getCpuFrequency(), captureTime, minimumRelativeConfidence);
	}

	public void setSID(int sidNumber, SIDChip sidConfig) {
		sids[sidNumber] = sidConfig;
		setDigiBoost(digiBoostEnabled);
	}

	/**
	 * Whether or not to enable Digiboost for all SID chips of model 8580.
	 *
	 * @param selected Whether or not to enable Digiboost.
	 */
	public void setDigiBoost(final boolean selected) {
		digiBoostEnabled = selected;

		for (SIDChip sidChip : sids) {
			if (sidChip != null) {
				sidChip.input(digiBoostEnabled ? sidChip.getInputDigiBoost() : 0);
			}
		}
	}

	public WhatsSidSupport getWhatsSidSupport() {
		return whatsSidSupport;
	}

}