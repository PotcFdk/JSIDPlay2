package sidplay.fingerprinting;

import java.io.IOException;

public interface IFingerprintMatcher {

	MusicInfoWithConfidenceBean match(WavBean wavBean) throws IOException;

}
