package sidplay.audio.processor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

public class AudioProcessorDriver implements AudioDriver {

	private AudioDriver audioDriver;

	private List<AudioProcessor> audioProcessors;

	public AudioProcessorDriver(AudioDriver audioDriver) {
		this.audioDriver = audioDriver;
		this.audioProcessors = new ArrayList<>();
	}

	public List<AudioProcessor> getAudioProcessors() {
		return audioProcessors;
	}

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		audioDriver.open(cfg, recordingFilename, cpuClock);
		for (AudioProcessor audioProcessor : audioProcessors) {
			audioProcessor.prepare(cfg);
		}
	}

	@Override
	public void write() throws InterruptedException {
		for (AudioProcessor audioProcessor : audioProcessors) {
			audioProcessor.process(audioDriver.buffer());
		}
		audioDriver.write();
	}

	@Override
	public ByteBuffer buffer() {
		return audioDriver.buffer();
	}

	@Override
	public void close() {
		audioDriver.close();
	}

	@Override
	public boolean isRecording() {
		return audioDriver.isRecording();
	}

	@Override
	public String getExtension() {
		return audioDriver.getExtension();
	}
	
	@Override
	public void pause() {
		audioDriver.pause();
	}
	
}
