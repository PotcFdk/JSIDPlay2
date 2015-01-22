package de.haendel.jsidplay2;

import static android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED;
import static android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;

public class JSIDPlay2Service extends Service implements OnPreparedListener,
		OnErrorListener, OnCompletionListener {

	private static final String JSIDPLAY2_FOLDER = "Download";
	private static final String JSIDPLAY2_JS2 = "jsidplay2.js2";

	public static class PlayListEntry {

		private String resource;

		private PlayListEntry(String resource) {
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

	private IConfiguration configuration;
	private boolean randomized;

	private List<PlayListEntry> playList;
	private int currentSong;
	private Random rnd;

	private final IBinder jsidplay2Binder = new JSIDPlay2Binder();
	private MediaPlayer player;

	public void setConfiguration(IConfiguration configuration) {
		this.configuration = configuration;
	}

	public void setRandomized(boolean randomized) {
		this.randomized = randomized;
	}

	public List<PlayListEntry> getPlayList() {
		return this.playList;
	}

	public void onCreate() {
		super.onCreate();

		// initialize playlist
		playList = new ArrayList<PlayListEntry>();
		currentSong = -1;
		rnd = new Random(System.currentTimeMillis());

		player = createMediaPlayer();
	}

	@Override
	public IBinder onBind(Intent intent) {
		try {
			load();
		} catch (UnsupportedEncodingException e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(), e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(), e.getMessage(), e);
		} catch (SecurityException e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(), e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(), e.getMessage(), e);
		} catch (IOException e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(), e.getMessage(), e);
		} catch (URISyntaxException e) {
			Log.e(JSIDPlay2Service.class.getSimpleName(), e.getMessage(), e);
		}
		return jsidplay2Binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		stopMediaPlayer(player);
		destroyMediaPlayer(player);
		return false;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(getPackageName(), String.format("Error(%s%s)", what, extra));
		switch (what) {
		case MEDIA_ERROR_SERVER_DIED:
			Toast.makeText(this, "MEDIA_ERROR_SERVER_DIED", Toast.LENGTH_SHORT)
					.show();
			break;
		case MEDIA_ERROR_UNKNOWN:
			Toast.makeText(this, "MEDIA_ERROR_UNKNOWN", Toast.LENGTH_SHORT)
					.show();
			break;
		default:
			break;
		}
		player.reset();
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playNextSong();
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		mediaPlayer.start();
	}

	public void playSong(PlayListEntry entry) {
		File file = new File(entry.getResource());
		Toast.makeText(this, file.getName(), Toast.LENGTH_SHORT).show();

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
					"Error setting data source!", e);
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

	public void stop() {
		Toast.makeText(this, "JSIDPlay2 Stopped...", Toast.LENGTH_SHORT).show();
		stopMediaPlayer(player);
	}

	public PlayListEntry add(String resource)
			throws UnsupportedEncodingException, IOException {
		PlayListEntry entry = new PlayListEntry(resource);
		playList.add(entry);

		save();
		return entry;
	}

	public void removeLast() throws UnsupportedEncodingException, IOException {
		PlayListEntry entry = getLast();
		if (entry != null) {
			playList.remove(entry);
			save();
		}
	}

	private MediaPlayer createMediaPlayer() {
		MediaPlayer mp = new MediaPlayer();
		mp.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mp.setOnPreparedListener(this);
		mp.setOnCompletionListener(this);
		mp.setOnErrorListener(this);
		return mp;
	}

	private void stopMediaPlayer(MediaPlayer mp) {
		if (mp.isPlaying()) {
			mp.stop();
		}
	}

	private void destroyMediaPlayer(MediaPlayer mp) {
		mp.release();
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

	private PlayListEntry getLast() {
		return playList.size() > 0 ? playList.get(playList.size() - 1) : null;
	}

	private void load() throws UnsupportedEncodingException, IOException,
			IllegalArgumentException, SecurityException, IllegalStateException,
			URISyntaxException {
		File sdRootDir = Environment.getExternalStorageDirectory();
		File playlistFile = new File(new File(sdRootDir, JSIDPLAY2_FOLDER),
				JSIDPLAY2_JS2);
		if (!playlistFile.exists()) {
			playlistFile.createNewFile();
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(playlistFile), "ISO-8859-1"));
		try {
			String line;
			while ((line = r.readLine()) != null) {
				// assuming JSIDPlay2 style - most often relative to HVSC
				String resource = (!line.startsWith("/C64Music") ? "/C64Music"
						: "") + line;
				playList.add(new PlayListEntry(resource));
			}
		} finally {
			r.close();
		}
	}

	private void save() throws UnsupportedEncodingException, IOException {
		File sdRootDir = Environment.getExternalStorageDirectory();
		File playlistFile = new File(new File(sdRootDir, JSIDPLAY2_FOLDER),
				JSIDPLAY2_JS2);
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(playlistFile), "ISO-8859-1"));
		try {
			for (PlayListEntry playListEntry : playList) {
				w.write(playListEntry.getResource());
				w.write('\n');
			}
		} finally {
			w.close();
		}
	}

}
