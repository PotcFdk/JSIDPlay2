package c64jukebox;

import java.applet.Applet;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.ConsolePlayer;
import sidplay.consoleplayer.Emulation;
import sidplay.consoleplayer.Output;
import sidplay.ini.IniConfig;

@SuppressWarnings("serial")
public class SIDPlay extends Applet {
	private IniConfig config;
	private ConsolePlayer cp;
	private Output fOut = Output.OUT_SOUNDCARD;
	private Emulation fEmu = Emulation.EMU_RESID;

	private final HashMap<String, SidTune> map = new HashMap<String, SidTune>();

	//
	// Applet methods
	//

	@Override
	public void init() {
		config = new IniConfig();
		cp = new ConsolePlayer(config);
		callJavaScript("javascript:init()");
	}

	@Override
	public void start() {
		// autostart if playsid parameter is set initially
		if (getAppletContext() != null) {
			playSID(getParameter("playsid"), -1);
		}
		callJavaScript("javascript:start()");
	}

	@Override
	public void stop() {
		cp.stopC64();
		callJavaScript("javascript:stop()");
	}

	@Override
	public void destroy() {
		callJavaScript("javascript:destroy()");
	}

	private void callJavaScript(String spec) {
		try {
			if (getAppletContext() != null) {
				getAppletContext().showDocument(new URL(spec));
			}
		} catch (final MalformedURLException e) {
			// ignore silently
		}
	}

	//
	// Methods wanted by Téli Sándor
	//

	/**
	 * Play a tune starting with the given song number.
	 * 
	 * @param urlName
	 *            tune URL
	 * @param songNum
	 *            song number to start with
	 */
	public void playSID(final String urlName, final int songNum) {
		if (urlName == null) {
			return;
		}
		// eventually stop last run
		cp.stopC64();

		// start new song
		String[] args;
		if (songNum == -1) {
			args = new String[] { urlName };
		} else {
			args = new String[] { "-o" + songNum, urlName };
		}
		if (!cp.args(args)) {
			return;
		}
		cp.setOutput(fOut);
		cp.setEmulation(fEmu);
		cp.startC64();
	}

	/**
	 * Stop Player.
	 */
	public void stopSID() {
		cp.stopC64();
	}

	/**
	 * Go to next song number.
	 */
	public void nextSong() {
		cp.nextSong();
	}

	/**
	 * Go to previous song number.
	 */
	public void previousSong() {
		cp.previousSong();
	}

	/**
	 * Use JSIDPlay2 emulation for the next song.
	 */
	public void useEmulation() {
		fOut = Output.OUT_SOUNDCARD;
		fEmu = Emulation.EMU_RESID;
	}

	/**
	 * Use HardSID4U for the next song.
	 */
	public void useHardSID() {
		fOut = Output.OUT_NULL;
		fEmu = Emulation.EMU_HARDSID;
	}

	/**
	 * Pause/Continue the player.
	 */
	public void pauseOrContinueSID() {
		cp.pause();
	}

	/**
	 * Get the current player state.
	 * 
	 * @return the player state
	 */
	public int stateSID() {
		return cp.stateProperty().get().ordinal();
	}

	/**
	 * Set the device numbers to be used for HardSID4U (for 6581 and 8580).
	 * 
	 * @param hardsid6581
	 *            device number to be used for 6581
	 * @param hardsid8580
	 *            device number to be used for 8580
	 */
	public void setChipDevice(final int hardsid6581, final int hardsid8580) {
		config.getEmulation().setHardsid6581(hardsid6581);
		config.getEmulation().setHardsid8580(hardsid8580);
	}

	/**
	 * How many songs does the tune consist of?
	 * 
	 * @param path
	 *            tune path
	 * @return the song count
	 */
	public int getSongCount(final String path) {
		final SidTune tune = getTune(path);
		return tune.getInfo().songs;
	}

	/**
	 * Which is the default song number of the tune to be played?
	 * 
	 * @param path
	 *            tune path
	 * @return the default song number
	 */
	public int getDefaultSong(final String path) {
		final SidTune tune = getTune(path);
		return tune.getInfo().startSong;
	}

	/**
	 * Get the song number, that is currently played.
	 * 
	 * @return current song number
	 */
	public int getCurrentSong() {
		return cp.getSelected();
	}

	/**
	 * Get the current time of the song that is currently played.
	 * 
	 * @return current time
	 */
	public int getCurrentTime() {
		return cp.getPlayer().time();
	}

	public int getHardSID_SID_Count() {
		return cp.getHardSIDCount();
	}

	//
	// Helpers
	//
	private SidTune getTune(final String path) {
		SidTune sidTuneMod = map.get(path);
		if (sidTuneMod != null) {
			return sidTuneMod;
		}
		try (InputStream stream = new URL(path).openConnection()
				.getInputStream()) {
			// load from URL (ui version)
			sidTuneMod = SidTune.load(stream);
			sidTuneMod.getInfo().file = null;
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
		if (sidTuneMod == null) {
			return null;
		}
		map.put(path, sidTuneMod);
		return sidTuneMod;
	}

}
