package sidplay.fingerprinting;

import java.io.IOException;

public interface FingerPrintMatcher {

	MusicInfoWithConfidenceBean match(WavBean wavBean) throws IOException;

}
