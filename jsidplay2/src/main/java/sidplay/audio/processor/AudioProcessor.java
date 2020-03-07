package sidplay.audio.processor;

import java.nio.ByteBuffer;

public interface AudioProcessor {

	void process(ByteBuffer buffer);

}
