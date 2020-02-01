package sidplay.audio.processor;

import java.nio.ByteBuffer;

import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;

public interface AudioProcessor {

	default void configure(IAudioSection audioSection) {
	};

	void prepare(AudioConfig cfg);

	void process(ByteBuffer buffer);

}
