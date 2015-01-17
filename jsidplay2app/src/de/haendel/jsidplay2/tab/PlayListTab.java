package de.haendel.jsidplay2.tab;

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
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;
import de.haendel.jsidplay2.request.TuneInfoRequest;

public abstract class PlayListTab implements OnCompletionListener {

	private static final String JSIDPLAY2_JS2 = "jsidplay2.js2";
	private static final String DOWNLOAD = "Download";
	private static final String PAR_RANDOM = "random";
	private static final String DEFAULT_RANDOM = Boolean.FALSE.toString();

	public static class PlayListEntry {

		private String resource;

		public PlayListEntry(String resource) {
			this.resource = resource;
		}

		public String getResource() {
			return resource;
		}
	}

	private Context context;
	private String appName;
	private IConfiguration configuration;
	private TabHost tabHost;
	private SharedPreferences preferences;

	private UIHelper ui;

	private Random rnd = new Random(System.currentTimeMillis());
	private List<PlayListEntry> list = new ArrayList<PlayListEntry>();
	private int idx = -1;

	private TableLayout favorites;
	private CheckBox random;
	private MediaPlayer mediaPlayer;

	public PlayListTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		this.context = activity;
		this.appName = appName;
		this.configuration = configuration;
		this.tabHost = tabHost;
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new UIHelper(preferences);
		tabHost.addTab(tabHost.newTabSpec(PlayListTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_playlist))
				.setContent(R.id.playlist));

		favorites = (TableLayout) activity.findViewById(R.id.favorites);
		random = (CheckBox) activity.findViewById(R.id.random);

		ui.setupCheckBox(random, PAR_RANDOM, DEFAULT_RANDOM);

	}

	public synchronized PlayListEntry add(String resource)
			throws URISyntaxException, IllegalArgumentException,
			SecurityException, IllegalStateException, IOException {
		final PlayListEntry entry = new PlayListEntry(resource);
		list.add(entry);
		TableRow row = new TableRow(context);
		row.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					idx = list.indexOf(entry);
					play(entry);
				} catch (IllegalArgumentException e) {
					Log.e(appName, e.getMessage(), e);
				} catch (SecurityException e) {
					Log.e(appName, e.getMessage(), e);
				} catch (IllegalStateException e) {
					Log.e(appName, e.getMessage(), e);
				} catch (IOException e) {
					Log.e(appName, e.getMessage(), e);
				} catch (URISyntaxException e) {
					Log.e(appName, e.getMessage(), e);
				}
				new TuneInfoRequest(appName, configuration, RequestType.INFO,
						entry.getResource()) {
					public String getString(String key) {
						key = key.replaceAll("[.]", "_");
						for (Field field : R.string.class.getDeclaredFields()) {
							if (field.getName().equals(key)) {
								try {
									return context.getString(field.getInt(null));
								} catch (IllegalArgumentException e) {
								} catch (IllegalAccessException e) {
								}
							}
						}
						return "???";
					}

					@Override
					protected void onPostExecute(List<Pair<String, String>> out) {
						if (out == null) {
							return;
						}
						getSidTab().viewTuneInfos(out);
					}
				}.execute();
			}
		});
		row.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.MATCH_PARENT));

		TextView col = new TextView(context);
		col.setText(resource);
		col.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));
		row.addView(col);
		row.setBackgroundResource(R.drawable.selector);

		favorites.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT));
		tabHost.setCurrentTabByTag(PlayListTab.class.getSimpleName());
		return entry;
	}

	public synchronized void load() throws UnsupportedEncodingException,
			IOException, IllegalArgumentException, SecurityException,
			IllegalStateException, URISyntaxException {
		File sdRootDir = Environment.getExternalStorageDirectory();
		File playlistFile = new File(new File(sdRootDir, DOWNLOAD),
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
				add((!line.startsWith("/C64Music") ? "/C64Music" : "") + line);
			}
		} finally {
			r.close();
		}
		idx = -1;
	}

	public synchronized void remove() throws UnsupportedEncodingException,
			IOException {
		PlayListEntry entry = getLast();
		if (entry != null) {
			list.remove(entry);

			favorites.removeViewAt(favorites.getChildCount() - 1);

		}
	}

	private PlayListEntry getLast() {
		return list.size() > 0 ? list.get(list.size() - 1) : null;
	}

	public synchronized void play(PlayListEntry next)
			throws IllegalArgumentException, SecurityException,
			IllegalStateException, IOException, URISyntaxException {
		startMediaPlayer(next);
	}

	public synchronized void stop() {
		stopMediaPlayer();
	}

	@Override
	public synchronized void onCompletion(MediaPlayer mp) {
		stopMediaPlayer();
		try {
			startMediaPlayer(next());
		} catch (IllegalArgumentException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (SecurityException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (URISyntaxException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	public synchronized void save() throws UnsupportedEncodingException,
			IOException {
		File sdRootDir = Environment.getExternalStorageDirectory();
		File playlistFile = new File(new File(sdRootDir, DOWNLOAD),
				JSIDPLAY2_JS2);
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(playlistFile), "ISO-8859-1"));
		try {
			for (PlayListEntry playListEntry : list) {
				w.write(playListEntry.getResource());
				w.write('\n');
			}
		} finally {
			w.close();
		}
	}

	public synchronized PlayListEntry next() {
		int next;
		if (random.isChecked()) {
			next = rnd.nextInt(list.size());
		} else {
			next = ++idx;
			;
		}
		return next < list.size() ? list.get(next) : null;
	}

	private void startMediaPlayer(PlayListEntry entry)
			throws IllegalArgumentException, SecurityException,
			IllegalStateException, IOException, URISyntaxException {
		stopMediaPlayer();
		if (entry == null) {
			return;
		}
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

		URI uri = new URI("http", configuration.getUsername() + ":"
				+ configuration.getPassword(), configuration.getHostname(),
				getNumber(configuration.getPort()),
				RequestType.CONVERT.getUrl() + entry.getResource(),
				query.toString(), null);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setDataSource(uri.toString());
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				mediaPlayer.setOnCompletionListener(PlayListTab.this);
				mediaPlayer.start();
			}
		});
		mediaPlayer.prepare();
	}

	private void stopMediaPlayer() {
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				mediaPlayer.release();
			} catch (Exception ex) {
				Log.e(appName, "Cannot stop media player!");
			}
		}
	}

	private int getNumber(String txt) {
		try {
			return Integer.parseInt(txt);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	protected abstract SidTab getSidTab();
}
