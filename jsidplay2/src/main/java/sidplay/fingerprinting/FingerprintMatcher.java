package sidplay.fingerprinting;

import java.io.IOException;

public interface FingerprintMatcher {

	MusicInfoWithConfidenceBean match(WavBean wavBean) throws IOException;

}
