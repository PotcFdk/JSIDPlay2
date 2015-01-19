package de.haendel.jsidplay2;

import static de.haendel.jsidplay2.config.IConfiguration.PAR_DEFAULT_MODEL;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_DEFAULT_PLAY_LENGTH;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_DIGI_BOOSTED_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_EMULATION;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_ENABLE_DATABASE;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_FREQUENCY;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_LOOP;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_STEREO_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_STEREO_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_SAMPLING_METHOD;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_SINGLE_SONG;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_STEREO_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_STEREO_FILTER_8580;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;

public class JSIDPlay2Service extends Service implements OnPreparedListener,
		OnErrorListener, OnCompletionListener {

	public static class PlayListEntry {

		private String resource;

		public PlayListEntry(String resource) {
			this.resource = resource;
		}

		public String getResource() {
			return resource;
		}
	}

	public class JSIDPlay2Binder extends Binder {
		public JSIDPlay2Service getService() {
			return JSIDPlay2Service.this;
		}
	}

	private Random rnd = new Random(System.currentTimeMillis());

	// media player
	private MediaPlayer player;
	// song list
	private List<PlayListEntry> playList;
	// current position
	private int currentSong;

	private final IBinder jsidplay2Binder = new JSIDPlay2Binder();

	private IConfiguration configuration;

	private boolean randomized;

	public void setConfiguration(IConfiguration configuration) {
		this.configuration = configuration;
	}

	public void setRandomized(boolean randomized) {
		this.randomized = randomized;
	}

	public void setList(List<PlayListEntry> list) {
		this.playList = list;
	}

	public void onCreate() {
		// create the service
		super.onCreate();

		// initialize position
		currentSong = -1;
		// create player
		player = new MediaPlayer();
		initMusicPlayer();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return jsidplay2Binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		stop();
		player.release();
		return false;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Toast.makeText(this, "JSIDPlay2 Completed...", Toast.LENGTH_SHORT)
				.show();
		playNextSong();
	}

	public void initMusicPlayer() {
		// set player properties
		player.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// start playback
		mp.start();
	}

	public void stop() {
		Toast.makeText(this, "JSIDPlay2 Stopped...", Toast.LENGTH_SHORT).show();
		if (player.isPlaying()) {
			player.stop();
		}
	}

	public void playSong(PlayListEntry entry) {
		File file = new File(entry.getResource());
		Toast.makeText(this, file.getName(), Toast.LENGTH_SHORT).show();

		// play a song
		player.reset();

		// get song
		currentSong = playList.indexOf(entry);

		if (currentSong == -1) {
			return;
		}

		try {
			URI uri = getURI(configuration, entry.getResource());
			player.setDataSource(getApplicationContext(),
					Uri.parse(uri.toString()));
		} catch (Exception e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(),
					"Error setting data source", e);
		}
		player.prepareAsync();
	}

	public void playNextSong() {
		if (randomized) {
			currentSong = rnd.nextInt(playList.size());
		} else {
			currentSong = currentSong + 1 < playList.size() ? currentSong + 1
					: -1;
		}
		if (currentSong == -1) {
			return;
		}

		playSong(playList.get(currentSong));
	}

	private URI getURI(IConfiguration configuration, String resource)
			throws URISyntaxException {
		StringBuilder query = new StringBuilder();
		query.append(PAR_EMULATION + "=" + configuration.getEmulation() + "&");
		query.append(PAR_ENABLE_DATABASE + "="
				+ configuration.isEnableDatabase() + "&");
		query.append(PAR_DEFAULT_PLAY_LENGTH + "="
				+ getNumber(configuration.getDefaultLength()) + "&");
		query.append(PAR_DEFAULT_MODEL + "=" + configuration.getDefaultModel()
				+ "&");
		query.append(PAR_SINGLE_SONG + "=" + configuration.isSingleSong() + "&");
		query.append(PAR_LOOP + "=" + configuration.isLoop() + "&");

		query.append(PAR_FILTER_6581 + "=" + configuration.getFilter6581()
				+ "&");
		query.append(PAR_FILTER_8580 + "=" + configuration.getFilter8580()
				+ "&");
		query.append(PAR_RESIDFP_FILTER_6581 + "="
				+ configuration.getReSIDfpFilter6581() + "&");
		query.append(PAR_RESIDFP_FILTER_8580 + "="
				+ configuration.getReSIDfpFilter8580() + "&");

		query.append(PAR_STEREO_FILTER_6581 + "="
				+ configuration.getStereoFilter6581() + "&");
		query.append(PAR_STEREO_FILTER_8580 + "="
				+ configuration.getStereoFilter8580() + "&");
		query.append(PAR_RESIDFP_STEREO_FILTER_6581 + "="
				+ configuration.getReSIDfpStereoFilter6581() + "&");
		query.append(PAR_RESIDFP_STEREO_FILTER_8580 + "="
				+ configuration.getReSIDfpStereoFilter8580() + "&");
		query.append(PAR_DIGI_BOOSTED_8580 + "="
				+ configuration.isDigiBoosted8580() + "&");
		query.append(PAR_SAMPLING_METHOD + "="
				+ configuration.getSamplingMethod() + "&");
		query.append(PAR_FREQUENCY + "=" + configuration.getFrequency());

		return new URI("http", configuration.getUsername() + ":"
				+ configuration.getPassword(), configuration.getHostname(),
				getNumber(configuration.getPort()),
				RequestType.CONVERT.getUrl() + resource, query.toString(), null);
	}

	private int getNumber(String txt) {
		try {
			return Integer.parseInt(txt);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
