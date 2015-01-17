package de.haendel.jsidplay2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import de.haendel.jsidplay2.JSIDPlay2RESTRequest.RequestType;

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
				dir.getCanonicalPath()) {

			@Override
			protected void onPostExecute(List<String> childs) {
				if (childs == null) {
					return;
				}
				viewDirectory(childs, filter);
			}
		}.execute(filter);
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
					if (dirEntry.endsWith("/")) {
						new DirectoryRequest(appName, configuration,
								RequestType.DIRECTORY, file.getCanonicalPath()) {

							@Override
							protected void onPostExecute(List<String> childs) {
								if (childs == null) {
									return;
								}
								viewDirectory(childs, filter);
							}
						}.execute(filter);
					} else {
						getSidTab().setCurrentTune(file.getCanonicalPath());
						new PhotoRequest(appName, configuration,
								RequestType.PHOTO, file.getCanonicalPath()) {
							@Override
							protected void onPostExecute(byte[] photo) {
								if (photo == null) {
									return;
								}
								getSidTab().viewPhoto(photo);
							}
						}.execute();
						new TuneInfoRequest(appName, configuration,
								RequestType.INFO, file.getCanonicalPath()) {
							public String getString(String key) {
								key = key.replaceAll("[.]", "_");
								for (Field field : R.string.class
										.getDeclaredFields()) {
									if (field.getName().equals(key)) {
										try {
											return context.getString(field
													.getInt(null));
										} catch (IllegalArgumentException e) {
										} catch (IllegalAccessException e) {
										}
									}
								}
								return "???";
							}

							@Override
							protected void onPostExecute(
									List<Pair<String, String>> out) {
								if (out == null) {
									return;
								}
								getSidTab().viewTuneInfos(out);
							}
						}.execute();
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

	protected abstract SidTab getSidTab();
}
