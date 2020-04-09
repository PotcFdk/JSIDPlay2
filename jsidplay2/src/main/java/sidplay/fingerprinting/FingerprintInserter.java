package sidplay.fingerprinting;

import java.io.IOException;

import libsidplay.sidtune.SidTune;

public interface FingerprintInserter {

	void insert(SidTune tune, String collectionFilename, String recordingFilename) throws IOException;

}
