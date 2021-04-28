package sidplay.audio.processors;

import java.nio.ByteBuffer;

public interface AudioProcessor {

	void process(ByteBuffer buffer);

}
