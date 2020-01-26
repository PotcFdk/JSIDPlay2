package sidplay.audio.processor;

import java.nio.ShortBuffer;

import sidplay.audio.AudioConfig;

public interface AudioProcessor {

	void prepare(ShortBuffer sampleBuffer, AudioConfig cfg);

	void process(ShortBuffer buffer);

}
