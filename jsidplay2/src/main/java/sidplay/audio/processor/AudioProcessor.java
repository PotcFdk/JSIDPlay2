package sidplay.audio.processor;

import java.nio.ByteBuffer;

import sidplay.audio.AudioConfig;

public interface AudioProcessor {

	void prepare(AudioConfig cfg);

	void process(ByteBuffer buffer);

}
