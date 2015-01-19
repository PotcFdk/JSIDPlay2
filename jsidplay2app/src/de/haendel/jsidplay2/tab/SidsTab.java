package de.haendel.jsidplay2.tab;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.DirectoryRequest;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;

public abstract class SidsTab {
	static final String TUNE_FILTER = ".*\\.(sid|dat|mus|str)$";

	ListView directory;

	private Context context;
	private String appName;
	private IConfiguration configuration;
	private TabHost tabHost;
	private SharedPreferences preferences;

	private UIHelper ui;

	public SidsTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		this.context = activity;
		this.appName = appName;
		this.configuration = configuration;
		this.tabHost = tabHost;
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new UIHelper(preferences);
		tabHost.addTab(tabHost.newTabSpec(SidsTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_sids))
				.setContent(R.id.sids));

		directory = (ListView) activity.findViewById(R.id.directory);

		try {
			requestDirectory(new File("/"), SidsTab.TUNE_FILTER);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	public void requestDirectory(final File dir, final String filter)
			throws IOException {
		new DirectoryRequest(appName, configuration, RequestType.DIRECTORY,
				dir.getCanonicalPath(), filter) {

			@Override
			protected void onPostExecute(List<String> childs) {
				if (childs == null) {
					return;
				}
				viewDirectory(childs, filter);
			}
		}.execute();
	}

	private void viewDirectory(List<String> childs, final String filter) {
		directory.setAdapter(new ArrayAdapter<String>(context,
				android.R.layout.simple_list_item_1, childs));
		directory.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				final String dirEntry = (String) parent
						.getItemAtPosition(position);
				File file = new File(dirEntry);
				try {
					String canonicalPath = file.getCanonicalPath();
					if (dirEntry.endsWith("/")) {
						new DirectoryRequest(appName, configuration,
								RequestType.DIRECTORY, canonicalPath, filter) {

							@Override
							protected void onPostExecute(List<String> childs) {
								if (childs == null) {
									return;
								}
								viewDirectory(childs, filter);
							}
						}.execute();
					} else {
						showSid(canonicalPath);
						tabHost.setCurrentTabByTag(SidTab.class.getSimpleName());
					}
				} catch (IOException e) {
					Log.e(appName, e.getMessage(), e);
				}
			}

		});
	}

	public void viewDirectory(final File dir, final String filter) {
		try {
			requestDirectory(dir, filter);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	protected abstract void showSid(String canonicalPath);

}

