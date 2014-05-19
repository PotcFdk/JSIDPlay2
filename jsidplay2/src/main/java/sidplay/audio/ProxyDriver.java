package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Proxy driver to use two different sound drivers.
 * 
 * @author Ken
 * 
 */
public class ProxyDriver extends AudioDriver {
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
	public void open(final AudioConfig cfg) throws LineUnavailableException,
			IOException, UnsupportedAudioFileException {
		driverOne.open(cfg);
		driverTwo.open(cfg);
	}

	@Override
	public void pause() {
		driverOne.pause();
		driverTwo.pause();
	}

	@Override
	public void write() throws InterruptedException {
		driverOne.write();
		// Driver two's buffer get the content of driver one's buffer
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
		// Driver one's buffer gets filled by JSIDPlay2
		return driverOne.buffer();
	}

	@Override
	public synchronized void fastForward() {
		driverOne.fastForward();
		driverTwo.fastForward();
	}

	@Override
	public synchronized void normalSpeed() {
		driverOne.normalSpeed();
		driverTwo.normalSpeed();
	}

	@Override
	public void setRecordingFilenameProvider(
			RecordingFilenameProvider recordingFilenameProvider) {
		driverOne.setRecordingFilenameProvider(recordingFilenameProvider);
		driverTwo.setRecordingFilenameProvider(recordingFilenameProvider);
	}
}
