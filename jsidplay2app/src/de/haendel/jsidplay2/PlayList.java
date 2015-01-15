package de.haendel.jsidplay2;

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

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class PlayList implements OnCompletionListener {

	private Random random = new Random(System.currentTimeMillis());

	private static final String JSIDPLAY2_JS2 = "jsidplay2.js2";
	private static final String DOWNLOAD = "Download";

	public static class PlayListEntry {

		private String resource;

		public PlayListEntry(String resource) {
			this.resource = resource;
		}

		public String getResource() {
			return resource;
		}
	}

	private List<PlayListEntry> list = new ArrayList<PlayListEntry>();
	private int idx = -1;

	private MediaPlayer mediaPlayer;
	private MainActivity mainActivity;

	public PlayList(MainActivity activity) {
		this.mainActivity = activity;
	}

	public synchronized PlayListEntry add(String resource)
			throws URISyntaxException, IllegalArgumentException,
			SecurityException, IllegalStateException, IOException {
		final PlayListEntry entry = new PlayListEntry(resource);
		list.add(entry);
		TableRow row = new TableRow(mainActivity);
		row.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					idx = list.indexOf(entry);
					play(entry);
				} catch (IllegalArgumentException e) {
					Log.e(mainActivity.appName, e.getMessage(), e);
				} catch (SecurityException e) {
					Log.e(mainActivity.appName, e.getMessage(), e);
				} catch (IllegalStateException e) {
					Log.e(mainActivity.appName, e.getMessage(), e);
				} catch (IOException e) {
					Log.e(mainActivity.appName, e.getMessage(), e);
				} catch (URISyntaxException e) {
					Log.e(mainActivity.appName, e.getMessage(), e);
				}
				new TuneInfoRequest(mainActivity.appName,
						mainActivity.connection, MainActivity.REST_INFO
								+ entry.getResource()) {
					public String getString(String key) {
						key = key.replaceAll("[.]", "_");
						for (Field field : R.string.class.getDeclaredFields()) {
							if (field.getName().equals(key)) {
								try {
									return mainActivity.getString(field
											.getInt(null));
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
						mainActivity.viewTuneInfos(out);
					}
				}.execute();
			}
		});
		row.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.MATCH_PARENT));

		TextView col = new TextView(mainActivity);
		col.setText(resource);
		col.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));
		row.addView(col);
		row.setBackgroundResource(R.drawable.selector);

		mainActivity.favorites.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT));
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

			mainActivity.favorites.removeViewAt(mainActivity.favorites
					.getChildCount() - 1);

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
			Log.e(mainActivity.appName, e.getMessage(), e);
		} catch (SecurityException e) {
			Log.e(mainActivity.appName, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(mainActivity.appName, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(mainActivity.appName, e.getMessage(), e);
		} catch (URISyntaxException e) {
			Log.e(mainActivity.appName, e.getMessage(), e);
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
		if (mainActivity.random.isSelected()) {
			next = random.nextInt(list.size());
		} else {
			next = ++idx;;
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
		query.append(MainActivity.PAR_EMULATION + "="
				+ mainActivity.emulation.getSelectedItem() + "&");
		query.append(MainActivity.PAR_ENABLE_DATABASE + "="
				+ mainActivity.enableDatabase.isChecked() + "&");
		query.append(MainActivity.PAR_DEFAULT_PLAY_LENGTH + "="
				+ getNumber(mainActivity.defaultLength.getText().toString())
				+ "&");
		query.append(MainActivity.PAR_DEFAULT_MODEL + "="
				+ mainActivity.defaultModel.getSelectedItem() + "&");
		query.append(MainActivity.PAR_SINGLE_SONG + "="
				+ mainActivity.singleSong.isChecked() + "&");
		query.append(MainActivity.PAR_LOOP + "="
				+ mainActivity.loop.isChecked() + "&");

		query.append(MainActivity.PAR_FILTER_6581 + "="
				+ mainActivity.filter6581.getSelectedItem() + "&");
		query.append(MainActivity.PAR_FILTER_8580 + "="
				+ mainActivity.filter8580.getSelectedItem() + "&");
		query.append(MainActivity.PAR_RESIDFP_FILTER_6581 + "="
				+ mainActivity.reSIDfpFilter6581.getSelectedItem() + "&");
		query.append(MainActivity.PAR_RESIDFP_FILTER_8580 + "="
				+ mainActivity.reSIDfpFilter8580.getSelectedItem() + "&");

		query.append(MainActivity.PAR_STEREO_FILTER_6581 + "="
				+ mainActivity.stereoFilter6581.getSelectedItem() + "&");
		query.append(MainActivity.PAR_STEREO_FILTER_8580 + "="
				+ mainActivity.stereoFilter8580.getSelectedItem() + "&");
		query.append(MainActivity.PAR_RESIDFP_STEREO_FILTER_6581 + "="
				+ mainActivity.reSIDfpStereoFilter6581.getSelectedItem() + "&");
		query.append(MainActivity.PAR_RESIDFP_STEREO_FILTER_8580 + "="
				+ mainActivity.reSIDfpStereoFilter8580.getSelectedItem() + "&");
		query.append(MainActivity.PAR_DIGI_BOOSTED_8580 + "="
				+ mainActivity.digiBoosted8580.isChecked() + "&");
		query.append(MainActivity.PAR_SAMPLING_METHOD + "="
				+ mainActivity.samplingMethod.getSelectedItem() + "&");
		query.append(MainActivity.PAR_FREQUENCY + "="
				+ mainActivity.frequency.getSelectedItem());

		URI uri = new URI("http", mainActivity.connection.getUsername() + ":"
				+ mainActivity.connection.getPassword(),
				mainActivity.connection.getHostname(),
				getNumber(mainActivity.connection.getPort()),
				MainActivity.REST_CONVERT_URL + entry.getResource(),
				query.toString(), null);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setDataSource(uri.toString());
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				mediaPlayer.setOnCompletionListener(PlayList.this);
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
				Log.e(mainActivity.appName, "Cannot stop media player!");
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

}
