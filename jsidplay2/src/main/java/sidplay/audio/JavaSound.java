package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

public class JavaSound implements AudioDriver {

	private AudioConfig cfg;
	private AudioFormat audioFormat;
	private SourceDataLine dataLine;
	private ByteBuffer sampleBuffer;

	@Override
	public synchronized void open(final AudioConfig cfg, String recordingFilename)
			throws IOException, LineUnavailableException {
		int device = cfg.getDevice();
		List<Info> devices = getDevices();
		open(cfg, device >= 0 && device < devices.size() ? devices.get(device) : (Info) null);
	}

	public static final List<Info> getDevices() {
		List<Info> devices = new ArrayList<>();
		for (Info info : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(info);
			Line.Info lineInfo = new Line.Info(SourceDataLine.class);
			if (mixer.isLineSupported(lineInfo)) {
				devices.add(info);
			}
		}
		return devices;
	}

	public synchronized void open(final AudioConfig cfg, final Mixer.Info info)
			throws IOException, LineUnavailableException {
		this.cfg = cfg;
		boolean signed = true;
		boolean bigEndian = false;
		this.audioFormat = new AudioFormat(cfg.getFrameRate(), Short.SIZE, cfg.getChannels(), signed, bigEndian);
		setAudioDevice(info);
	}

	public synchronized void setAudioDevice(final Mixer.Info info) throws LineUnavailableException {
		// first close previous dataLine when it is already present
		close();
		dataLine = AudioSystem.getSourceDataLine(audioFormat, info);
		dataLine.open(dataLine.getFormat(), cfg.getBufferFrames() * Short.BYTES * cfg.getChannels());
		
		dataLine.start();
		
		// The actual buffer size for the open line may differ from the
		// requested buffer size, therefore
		cfg.setBufferFrames(dataLine.getBufferSize() / Short.BYTES / cfg.getChannels());

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
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
