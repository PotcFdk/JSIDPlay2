package sidplay.audio;

import libsidplay.sidtune.SidTune;

public interface RecordingFilenameProvider {
	String getFilename(SidTune tune);
}
