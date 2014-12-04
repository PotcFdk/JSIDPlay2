package c64jukebox;

import java.applet.Applet;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import libsidplay.Player;
import libsidplay.common.Emulation;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.audio.Audio;
import sidplay.ini.IniConfig;

@SuppressWarnings("serial")
public class SIDPlay extends Applet {
	private IniConfig config;
	private Player player;

	private final HashMap<String, SidTune> map = new HashMap<String, SidTune>();

	//
	// Applet methods
	//

	@Override
	public void init() {
		config = new IniConfig();
		player = new Player(config);
		callJavaScript("javascript:init()");
	}

	@Override
	public void start() {
		// autostart if playsid parameter is set initially
		if (getAppletContext() != null) {
			String startSong = getParameter("startSong");
			playSID(getParameter("playsid"),
					startSong != null ? Integer.valueOf(startSong) : null);
		}
		callJavaScript("javascript:start()");
	}

	@Override
	public void stop() {
		player.stopC64();
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
	public void playSID(final String urlName, final Integer songNum) {
		if (urlName == null) {
			return;
		}
		player.stopC64();
		player.setTune(getTune(urlName));
		player.getTune().setSelectedSong(songNum);
		player.startC64();
	}

	/**
	 * Stop Player.
	 */
	public void stopSID() {
		player.stopC64();
	}

	/**
	 * Go to next song number.
	 */
	public void nextSong() {
		player.nextSong();
	}

	/**
	 * Go to previous song number.
	 */
	public void previousSong() {
		player.previousSong();
	}

	/**
	 * Use JSIDPlay2 emulation for the next song.
	 */
	public void useEmulation() {
		config.getEmulation().setEmulation(Emulation.HARDSID);
		config.getAudio().setAudio(Audio.NONE);
	}

	/**
	 * Use HardSID4U for the next song.
	 */
	public void useHardSID() {
		config.getEmulation().setEmulation(Emulation.RESID);
		config.getAudio().setAudio(Audio.SOUNDCARD);
	}

	/**
	 * Pause/Continue the player.
	 */
	public void pauseOrContinueSID() {
		player.pause();
	}

	/**
	 * Get the current player state.
	 * 
	 * @return the player state
	 */
	public int stateSID() {
		return player.stateProperty().get().ordinal();
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
		return getTune(path).getInfo().getSongs();
	}

	/**
	 * Which is the default song number of the tune to be played?
	 * 
	 * @param path
	 *            tune path
	 * @return the default song number
	 */
	public int getDefaultSong(final String path) {
		return getTune(path).getInfo().getStartSong();
	}

	/**
	 * Get the song number, that is currently played.
	 * 
	 * @return current song number
	 */
	public int getCurrentSong() {
		return player.getPlayList().getCurrent();
	}

	/**
	 * Get the current time of the song that is currently played.
	 * 
	 * @return current time
	 */
	public int getCurrentTime() {
		return player.time();
	}

	public int getHardSID_SID_Count() {
		return player.getNumDevices();
	}

	//
	// Helpers
	//
	private SidTune getTune(final String url) {
		SidTune tune = map.get(url);
		if (tune != null) {
			return tune;
		}
		try (InputStream stream = new URL(url).openConnection()
				.getInputStream()) {
			tune = SidTune.load(url, stream);
			map.put(url, tune);
			return tune;
		} catch (IOException | SidTuneError e) {
			showStatus(e.getMessage());
			return null;
		}
	}

}
