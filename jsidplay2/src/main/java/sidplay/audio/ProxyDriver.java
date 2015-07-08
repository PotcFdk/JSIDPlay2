package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.LineUnavailableException;

/**
 * Proxy driver to use two different sound drivers.
 * 
 * @author Ken Händel
 * 
 */
public class ProxyDriver implements AudioDriver {
	private final AudioDriver driverOne;
	private final AudioDriver driverTwo;

	/**
	 * Create a proxy driver
	 * 
	 * @param driver1
	 *            sound driver, that buffer gest filled
	 * @param driver2
	 *            sound driver, that gets the copied sample buffer
	 */
	public ProxyDriver(final AudioDriver driver1, final AudioDriver driver2) {
		driverOne = driver1;
		driverTwo = driver2;
	}

	@Override
	public void open(final AudioConfig cfg, String recordingFilename)
			throws LineUnavailableException, IOException {
		driverOne.open(cfg, recordingFilename);
		driverTwo.open(cfg, recordingFilename);
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
		System.arraycopy(buffer().array(), 0, driverTwo.buffer().array(), 0,
				driverTwo.buffer().capacity());
		driverTwo.write();
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

}
