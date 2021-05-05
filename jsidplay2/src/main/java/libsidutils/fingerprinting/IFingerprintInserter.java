package libsidutils.fingerprinting;

import java.io.IOException;

import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;

public interface IFingerprintInserter {

	void insert(MusicInfoBean musicInfoBean, WAVBean wavBean) throws IOException;

}
