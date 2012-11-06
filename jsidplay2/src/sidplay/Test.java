package sidplay;

import java.io.File;
import java.util.List;

import libsidplay.Player;
import libsidplay.common.ISID2Types.CPUClock;
import libsidplay.sidtune.SidTune;
import resid_builder.ReSID;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ISIDDefs.ChipModel;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.JavaSound;
import sidplay.ini.IniConfig;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFilterSection;

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

		// Create player and apply the tune
		Player player = new Player();
		player.setTune(tune);

		// Read filter settings and create filter
		final IConfig iniCfg = new IniConfig();
		IFilterSection filter6581 = null;
		IFilterSection filter8580 = null;
		List<? extends IFilterSection> filters = iniCfg.getFilter();
		for (IFilterSection iFilterSection : filters) {
			if (iFilterSection.getName().equals(
					iniCfg.getEmulation().getFilter6581())) {
				filter6581 = iFilterSection;
			} else if (iFilterSection.getName().equals(
					iniCfg.getEmulation().getFilter8580())) {
				filter8580 = iFilterSection;
			}
		}

		// Customize player configuration
		player.setClock(CPUClock.PAL);

		// Get sound driver and apply to the player
		final AudioDriver driver = new JavaSound();
		final AudioConfig config = AudioConfig
				.getInstance(iniCfg.getAudio(), 1);
		driver.open(config);

		// Setup the SID emulation (not part of the player)
		final ReSIDBuilder rs = new ReSIDBuilder(config, player.getC64()
				.getClock().getCpuFrequency());
		rs.setOutput(driver);

		// Create SID chip of desired model (mono tunes need exactly one)
		final ReSID sid = (ReSID) rs.lock(player.getC64().getEventScheduler(),
				ChipModel.MOS6581);
		// Enable/apply filter to the SID emulation
		sid.setFilter(true);
		sid.filter(filter6581, filter8580);
		sid.sampling(player.getC64().getClock().getCpuFrequency(), iniCfg
				.getAudio().getFrequency(), iniCfg.getAudio().getSampling());

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
