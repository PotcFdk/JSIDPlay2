package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.processor.AudioProcessor;

/**
 * AudioProcessor driver to use several audio post processors short before
 * sample data gets written. Audio post processors must implement
 * {@link AudioProcessor}.
 * 
 * @author Ken Händel
 * 
 */
public class AudioProcessorDriver implements AudioDriver, VideoDriver {

	private AudioDriver audioDriver;

	private List<AudioProcessor> audioProcessors = new ArrayList<>();

	public AudioProcessorDriver(AudioDriver audioDriver) {
		this.audioDriver = audioDriver;
	}

	public List<AudioProcessor> getAudioProcessors() {
		return audioProcessors;
	}

	@Override
	public void pause() {
		audioDriver.pause();
	}

	@Override
	public void configure(IAudioSection audioSection) {
		audioDriver.configure(audioSection);
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
	public void accept(VIC vic) {
		if (audioDriver instanceof VideoDriver) {
			((VideoDriver) audioDriver).accept(vic);
		}
	}

	@Override
	public void close() {
		audioDriver.close();
	}

	@Override
	public ByteBuffer buffer() {
		return audioDriver.buffer();
	}

	@Override
	public boolean isRecording() {
		return audioDriver.isRecording();
	}

	@Override
	public String getExtension() {
		return audioDriver.getExtension();
	}

}
