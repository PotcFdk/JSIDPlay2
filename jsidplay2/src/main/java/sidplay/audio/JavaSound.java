package sidplay.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import libsidplay.sidtune.SidTune;

public class JavaSound extends AudioDriver {
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
	public synchronized void open(final AudioConfig cfg, SidTune tune)
			throws LineUnavailableException {
		int device = cfg.getDevice();
		ObservableList<Device> devices = getDevices();
		if (device < devices.size()) {
			open(cfg, devices.get(device).getInfo());
		} else {
			open(cfg, (Info) null);
		}
	}

	public synchronized void open(final AudioConfig cfg, final Mixer.Info info)
			throws LineUnavailableException {
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
			throws LineUnavailableException {
		// first close previous dataLine when it already present
		close();

		if (info == null) {
			dataLine = AudioSystem.getSourceDataLine(audioFormat);
		} else {
			dataLine = AudioSystem.getSourceDataLine(audioFormat, info);
		}
		dataLine.open(dataLine.getFormat(), cfg.bufferFrames * Short.BYTES
				* cfg.channels);
		// The actual buffer size for the open line may differ from the
		// requested buffer size, therefore
		cfg.bufferFrames = dataLine.getBufferSize() / Short.BYTES
				/ cfg.channels;

		/*
		 * Write to audio device often. We make the sample buffer size divisible
		 * by 64 to ensure that all fast forward factors can be handled. (32x
		 * speed, 2 channels)
		 */
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES
				* cfg.channels);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public synchronized void write() throws InterruptedException {
		// in pause mode next call of write continues
		if (!dataLine.isActive()) {
			dataLine.start();
		}

		final int len;
		if (fastForward > 1) {
			sampleBuffer.rewind();
			int newLen = 0;
			int[] val = new int[audioFormat.getChannels()];
			int j = 0;
			/* for each short-formatted sample in the buffer... */
			while (sampleBuffer.position() < sampleBuffer.capacity()) {
				/* accumulate each interleaved channel into its own accumulator */
				for (int c = 0; c < audioFormat.getChannels(); c++) {
					val[c] += sampleBuffer.getShort();
				}

				/*
				 * once enough samples have been accumulated, write one to
				 * output
				 */
				j++;
				if (j == fastForward) {
					j = 0;

					for (int c = 0; c < audioFormat.getChannels(); c++) {
						sampleBuffer.putShort(newLen,
								(short) (val[c] / fastForward));
						newLen += Short.BYTES;
					}

					/* zero accumulator */
					Arrays.fill(val, 0);
				}
			}
			len = newLen;
		} else {
			len = sampleBuffer.capacity();
		}

		int bytesWritten = dataLine.write(sampleBuffer.array(), 0, len);
		if (bytesWritten != len) {
			throw new InterruptedException();
		}
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
		if (dataLine != null && dataLine.isActive()) {
			dataLine.stop();
			dataLine.flush();
			dataLine.close();
		}
	}

	public AudioFormat getAudioFormat() {
		return audioFormat;
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}
}
