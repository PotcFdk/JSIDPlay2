package sidplay.fingerprinting;

import java.io.IOException;

import libsidplay.sidtune.SidTune;

public interface IFingerprintInserter {

	void insert(SidTune tune, String infoDir, String recordingFilename) throws IOException;

}
