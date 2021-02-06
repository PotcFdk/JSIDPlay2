package libsidutils.fingerprinting;

import java.io.IOException;

import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WavBean;

public interface IFingerprintMatcher {

	MusicInfoWithConfidenceBean match(WavBean wavBean) throws IOException;

}
