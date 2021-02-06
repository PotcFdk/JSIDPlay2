package libsidutils.fingerprinting;

import java.io.IOException;

import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.WavBean;

public interface IFingerprintInserter {

	void insert(MusicInfoBean musicInfoBean, WavBean wavBean) throws IOException;

}
