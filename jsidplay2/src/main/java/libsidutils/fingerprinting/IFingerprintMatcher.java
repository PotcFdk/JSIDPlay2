package libsidutils.fingerprinting;

import java.io.IOException;

import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;

public interface IFingerprintMatcher {

	MusicInfoWithConfidenceBean match(WAVBean wavBean) throws IOException;

}
