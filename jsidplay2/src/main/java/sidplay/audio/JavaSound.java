package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

public class JavaSound implements AudioDriver {
	public static final class Device {
		private final Info info;

		public Device(Info info) {
			this.info = info;
		}

		public Info getInfo() {
			return info;
		}

		@Override
		public String toString() {
			return info.getName();
		}
	}

	private AudioConfig cfg;
	private AudioFormat audioFormat;
	private SourceDataLine dataLine;
	private ByteBuffer sampleBuffer;

	@Override
	public synchronized void open(final AudioConfig cfg,
			String recordingFilename) throws IOException {
		int device = cfg.getDevice();
		ObservableList<Device> devices = getDevices();
		if (device < devices.size()) {
			open(cfg, devices.get(device).getInfo());
		} else {
			open(cfg, (Info) null);
		}
	}

	public synchronized void open(final AudioConfig cfg, final Mixer.Info info)
			throws IOException {
		this.cfg = cfg;
		boolean signed = true;
		boolean bigEndian = false;
		this.audioFormat = new AudioFormat(cfg.frameRate, Short.SIZE,
				cfg.channels, signed, bigEndian);
		setAudioDevice(info);
	}

	public static final ObservableList<Device> getDevices() {
		ObservableList<Device> devices = FXCollections
				.<Device> observableArrayList();
		for (Info info : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(info);
			Line.Info lineInfo = new Line.Info(SourceDataLine.class);
			if (mixer.isLineSupported(lineInfo)) {
				devices.add(new Device(info));
			}
		}
		return devices;
	}

	public synchronized void setAudioDevice(final Mixer.Info info)
			throws IOException {
		// first close previous dataLine when it is already present
		close();
		try {
			dataLine = AudioSystem.getSourceDataLine(audioFormat, info);
			dataLine.open(dataLine.getFormat(), cfg.bufferFrames * Short.BYTES
					* cfg.channels);

			// The actual buffer size for the open line may differ from the
			// requested buffer size, therefore
			cfg.bufferFrames = dataLine.getBufferSize() / Short.BYTES
					/ cfg.channels;

			sampleBuffer = ByteBuffer.allocate(
					cfg.getChunkFrames() * Short.BYTES * cfg.channels).order(
					ByteOrder.LITTLE_ENDIAN);
		} catch (LineUnavailableException e) {
			// When a requested line is already in use by another application
			throw new IOException(e);
		}
	}

	@Override
	public synchronized void write() throws InterruptedException {
		// in pause mode next call of write continues
		if (!dataLine.isActive()) {
			dataLine.start();
		}
		dataLine.write(sampleBuffer.array(), 0, sampleBuffer.capacity());
	}

	/**
	 * Estimate the length of audio data before we run out
	 * 
	 * @return playback time in ms
	 */
	public synchronized int getRemainingPlayTime() {
		int bytesPerFrame = dataLine.getFormat().getChannels() * Short.BYTES;
		int framesPlayed = dataLine.available() / bytesPerFrame;
		int framesTotal = dataLine.getBufferSize() / bytesPerFrame;
		int framesNotYetPlayed = framesTotal - framesPlayed;
		return framesNotYetPlayed * 1000 / framesTotal;
	}

	@Override
	public synchronized void pause() {
		if (dataLine.isActive()) {
			dataLine.stop();
		}
	}

	@Override
	public synchronized void close() {
		if (dataLine == null) {
			return;
		}
		if (dataLine.isActive()) {
			dataLine.stop();
			dataLine.flush();
		}
		if (dataLine.isOpen()) {
			dataLine.close();
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}
}
