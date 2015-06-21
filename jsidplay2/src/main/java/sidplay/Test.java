package sidplay;

import java.io.File;
import java.io.IOException;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.ini.IniConfig;

/**
 * This minimal test class demonstrates the use of the C64 music player. It
 * shows how to integrate the player in other java programs.
 * 
 * @author Ken Händel
 * 
 */
public class Test {
	/**
	 * Play a tune.
	 * 
	 * @param filename
	 *            the filename of the tune
	 * @throws SidTuneError
	 * @throws IOException
	 */
	public Test(final String filename) throws IOException, SidTuneError {
		// Load tune
		final SidTune tune = SidTune.load(new File(filename));

		// Create player
		final Player player = new Player(new IniConfig());

		// start C64
		player.play(tune);
	}

	public static void main(final String[] args) throws IOException,
			SidTuneError {
		if (args.length < 1) {
			System.err.println("Missing argument: <filename>");
			System.exit(-1);
		}
		new Test(args[0]);
	}
}
