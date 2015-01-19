package de.haendel.jsidplay2.tab;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.haendel.jsidplay2.JSIDPlay2Service.PlayListEntry;
import de.haendel.jsidplay2.MainActivity;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;

public abstract class PlayListTab {

	public class PlayListUIHelper extends UIHelper {

		public PlayListUIHelper(SharedPreferences preferences) {
			super(preferences);
		}

		@Override
		protected void checkBoxUpdated(String parName, boolean newValue) {
			if (parName.equals(PAR_RANDOM)) {
				context.setRandomized(newValue);
			}
		}
	}

	/**
	 * Playlist filename.
	 */
	private static final String JSIDPLAY2_JS2 = "jsidplay2.js2";
	/**
	 * Folder of the playlist.
	 */
	private static final String DOWNLOAD = "Download";

	private static final String PAR_RANDOM = "random";
	private static final String DEFAULT_RANDOM = Boolean.FALSE.toString();

	private MainActivity context;
	private String appName;
	private IConfiguration configuration;
	private TabHost tabHost;
	private SharedPreferences preferences;

	private UIHelper ui;

	private List<PlayListEntry> list = new ArrayList<PlayListEntry>();

	private TableLayout favorites;
	private CheckBox random;

	public PlayListTab(final MainActivity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		this.context = activity;
		this.appName = appName;
		this.configuration = configuration;
		this.tabHost = tabHost;
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new PlayListUIHelper(preferences);
		tabHost.addTab(tabHost.newTabSpec(PlayListTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_playlist))
				.setContent(R.id.playlist));

		favorites = (TableLayout) activity.findViewById(R.id.favorites);
		random = (CheckBox) activity.findViewById(R.id.random);

		ui.setupCheckBox(random, PAR_RANDOM, DEFAULT_RANDOM);

	}

	public PlayListEntry add(final String resource) {
		final PlayListEntry entry = new PlayListEntry(resource);
		list.add(entry);
		TableRow row = new TableRow(context);
		row.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				play(entry);
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

	public void load() throws UnsupportedEncodingException, IOException,
			IllegalArgumentException, SecurityException, IllegalStateException,
			URISyntaxException {
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
	}

	public void remove() throws UnsupportedEncodingException, IOException {
		PlayListEntry entry = getLast();
		if (entry != null) {
			list.remove(entry);

			favorites.removeViewAt(favorites.getChildCount() - 1);

		}
	}

	private PlayListEntry getLast() {
		return list.size() > 0 ? list.get(list.size() - 1) : null;
	}

	public void save() throws UnsupportedEncodingException, IOException {
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

	public List<PlayListEntry> getList() {
		return list;
	}

	protected abstract void play(PlayListEntry entry);

}
