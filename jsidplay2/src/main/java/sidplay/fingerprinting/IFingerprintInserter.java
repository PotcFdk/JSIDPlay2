package sidplay.fingerprinting;

import java.io.IOException;

public interface IFingerprintInserter {

	void insert(MusicInfoBean musicInfoBean, WavBean wavBean) throws IOException;

}
