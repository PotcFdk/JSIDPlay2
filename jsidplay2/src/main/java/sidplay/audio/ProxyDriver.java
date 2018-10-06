package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;

/**
 * Proxy driver to use two different sound or video drivers at the same time.
 * 
 * <B>Note:</B> Both driver's sample buffer must be equal in size.
 * 
 * @author Ken Händel
 * 
 */
public class ProxyDriver implements VideoDriver {
	private final AudioDriver driverOne;
	private final AudioDriver driverTwo;

	/**
	 * Create a proxy driver
	 * 
	 * @param driver1 sound driver, that buffer gets filled
	 * @param driver2 sound driver, that gets the copied sample buffer
	 */
	public ProxyDriver(final AudioDriver driver1, final AudioDriver driver2) {
		driverOne = driver1;
		driverTwo = driver2;
	}

	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		driverOne.open(cfg, recordingFilename, cpuClock);
		driverTwo.open(cfg, recordingFilename, cpuClock);
	}

	@Override
	public void pause() {
		driverOne.pause();
		driverTwo.pause();
	}

	@Override
	public void write() throws InterruptedException {
		driverOne.write();
		// Driver two's buffer gets the content of driver one's buffer
		System.arraycopy(buffer().array(), 0, driverTwo.buffer().array(), 0, driverTwo.buffer().capacity());
		driverTwo.write();
	}

	@Override
	public void accept(int[] bgraData) {
		if (driverOne instanceof VideoDriver) {
			((VideoDriver) driverOne).accept(bgraData);
		}
		if (driverTwo instanceof VideoDriver) {
			((VideoDriver) driverTwo).accept(bgraData);
		}
	}

	@Override
	public void close() {
		driverOne.close();
		driverTwo.close();
	}

	@Override
	public ByteBuffer buffer() {
		// Driver one's buffer gets filled, while driver two gets a copy in
		// method write()
		return driverOne.buffer();
	}

	public AudioDriver getDriverOne() {
		return driverOne;
	}

	public AudioDriver getDriverTwo() {
		return driverTwo;
	}
}
