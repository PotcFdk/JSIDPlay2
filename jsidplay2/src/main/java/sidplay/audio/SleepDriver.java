package sidplay.audio;

import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.config.IAudioSystemProperties.MAX_TIME_GAP;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;

/**
 * No sound output at all, but sleeps to make C64 time equal system time.
 * 
 * @author ken
 *
 */
public class SleepDriver implements AudioDriver {

	private CPUClock cpuClock;
	private EventScheduler context;

	private long startTime, time, startC64Time, c64Time;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.cpuClock = cpuClock;
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);

		startTime = 0;
		time = 0;
		startC64Time = 0;
		c64Time = 0;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
			startC64Time = context.getTime(Phase.PHI2);
		}
		time = System.currentTimeMillis() - startTime;
		c64Time = (long) ((context.getTime(Phase.PHI2) - startC64Time) * 1000 / cpuClock.getCpuFrequency());

		long sleepTime = c64Time - time;
		if (sleepTime > MAX_TIME_GAP) {
			try {
				// slow down video production to stay in sync with a possible viewer
				Thread.sleep(sleepTime - MAX_TIME_GAP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() {
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return false;
	}

}
