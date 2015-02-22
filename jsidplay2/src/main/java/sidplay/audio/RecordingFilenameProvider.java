package sidplay.audio;

import libsidplay.sidtune.SidTune;

/**
 * Filename provider for file recordings.
 * 
 * @author ken
 *
 */
public interface RecordingFilenameProvider {
	String getFilename(SidTune tune);
}
