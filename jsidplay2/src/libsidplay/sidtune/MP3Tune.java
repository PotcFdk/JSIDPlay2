package libsidplay.sidtune;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Special MP3 tune implementation. This is not a program present in the C64
 * memory, it is just a file played back by the jump3r library. However this C64
 * emulator requires tunes so this is a dummy to meet that requirement.
 * 
 * @author Ken
 * 
 */
public class MP3Tune extends SidTune {

	@Override
	public int placeProgramInMemory(byte[] c64buf) {
		// No program to load
		return -1;
	}

	@Override
	public void save(String destFileName, boolean overWriteFlag)
			throws IOException {
		// Saving is not possible
	}

	@Override
	public ArrayList<String> identify() {
		ArrayList<String> names = new ArrayList<String>();
		// The player is called jump3r ;-)
		names.add("jump3r");
		return names;
	}

	@Override
	public long getInitDelay() {
		// MP3 can play immediately
		return 0;
	}

	public static final SidTune load(final File f) throws SidTuneError {
		final MP3Tune s = new MP3Tune();
		// fill out some minimal information of an MP3 tune
		s.info.dataFileLen = (int) f.length();
		s.info.file = f;
		s.info.startSong = 1;
		s.info.songs = 1;
		return s;
	}

}
