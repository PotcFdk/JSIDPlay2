package sidplay;

import java.io.File;

import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.sidtune.SidTune;
import resid_builder.ReSID;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ChipModel;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.JavaSound;
import sidplay.ini.IniConfig;
import sidplay.ini.intf.IConfig;

/**
 * This test class demonstrates the use of the ReSID engine. It has only minimum
 * dependencies to show how to integrate the player in other java environments.
 * 
 * @author Ken Händel
 * 
 */
public class Test {
	/**
	 * Play a tune by filename
	 * 
	 * @param filename
	 *            the filename to load the tune
	 */
	public void playTune(final String filename) throws Exception {
		// Load tune and select first song
		final SidTune tune = SidTune.load(new File(filename));
		tune.selectSong(1);

		// Create default configuration
		final IConfig config = new IniConfig();
		
		// Create player and apply the tune
		Player player = new Player(config);
		player.setTune(tune);

		// Customize player configuration
		player.setClock(CPUClock.PAL);

		// Get sound driver and apply to the player
		final AudioDriver driver = new JavaSound();
		final AudioConfig audioConfig = AudioConfig.getInstance(
				config.getAudio(), 1);

		// Setup the SID emulation (not part of the player)
		final ReSIDBuilder rs = new ReSIDBuilder(audioConfig, player.getC64()
				.getClock().getCpuFrequency(), 0f, 0f);
		rs.setDriver(driver, null);

		// Create SID chip of desired model (mono tunes need exactly one)
		final ReSID sid = (ReSID) rs.lock(player.getC64().getEventScheduler(),
				ChipModel.MOS6581);
		// Apply filter to the SID emulation
		sid.setFilter(config);
		sid.setSampling(player.getC64().getClock().getCpuFrequency(), config
				.getAudio().getFrequency(), config.getAudio().getSampling());

		// Apply mono SID chip to the C64, then reset
		player.getC64().setSID(0, sid);
		player.reset();

		// Play forever
		while (true) {
			player.play(10000);
		}
	}

	public static void main(final String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Missing argument: <filename>");
			System.exit(-1);
		}
		final Test tst = new Test();
		tst.playTune(args[0]);
	}
}
