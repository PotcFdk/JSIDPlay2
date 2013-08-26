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
import sidplay.ConsolePlayer.OUTPUTS;
import sidplay.ConsolePlayer.SIDEMUS;
import sidplay.ini.IniConfig;

@SuppressWarnings("serial")
public class SIDPlay extends Applet {
	private ConsolePlayer cp;
	private String fUrlName;
	private OUTPUTS fOut = OUTPUTS.OUT_SOUNDCARD;
	private SIDEMUS fEmu = SIDEMUS.EMU_RESID;

	private final HashMap<String, SidTune> map = new HashMap<String, SidTune>();

	/**
	 * Start song number or -1 (default song)
	 */
	private int fSong;

	//
	// Applet methods
	//

	@Override
	public void init() {
		cp = new ConsolePlayer(new IniConfig());
		try {
			if (getAppletContext() != null) {
				getAppletContext().showDocument(new URL("javascript:init()"));
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		// autostart if playsid parameter is set initially
		if (getAppletContext() != null && getParameter("playsid") != null) {
			playSID(getParameter("playsid"), -1);
		}
		try {
			if (getAppletContext() != null) {
				getAppletContext().showDocument(new URL("javascript:start()"));
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		cp.stopC64();
		try {
			if (getAppletContext() != null) {
				getAppletContext().showDocument(new URL("javascript:stop()"));
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			if (getAppletContext() != null) {
				getAppletContext()
						.showDocument(new URL("javascript:destroy()"));
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
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
		// eventually stop last run
		cp.stopC64();

		// Next time player is used, the track is reset
		cp.resetTrack();

		// start new song
		fUrlName = urlName;
		fSong = songNum;

		String[] args;
		if (fSong == -1) {
			args = new String[] { fUrlName };
		} else {
			args = new String[] { "-o" + fSong, fUrlName };
		}
		if (cp.args(args) < 0) {
			return;
		}
		cp.startC64();
		cp.getDriverSettings().setOutput(fOut);
		cp.getDriverSettings().setSid(fEmu);
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
		fOut = OUTPUTS.OUT_SOUNDCARD;
		fEmu = SIDEMUS.EMU_RESID;
	}

	/**
	 * Use HardSID4U for the next song.
	 */
	public void useHardSID() {
		fOut = OUTPUTS.OUT_NULL;
		fEmu = SIDEMUS.EMU_HARDSID;
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
		return cp.getState().get();
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
		cp.getConfig().getEmulation().setHardsid6581(hardsid6581);
		cp.getConfig().getEmulation().setHardsid8580(hardsid8580);
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
		return cp.getTrack().getCurrentSong();
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
