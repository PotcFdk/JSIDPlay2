package de.haendel.jsidplay2.tab;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.TabBase;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.DirectoryRequest;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;

public abstract class SidsTab extends TabBase {

	private ListView directory;

	public SidsTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		super(activity, appName, configuration, tabHost);
		tabHost.addTab(tabHost.newTabSpec(SidsTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_sids))
				.setContent(R.id.sids));

		directory = (ListView) activity.findViewById(R.id.directory);

		try {
			requestDirectory(new File("/"));
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	public void requestDirectory(final File dir)
			throws IOException {
		new DirectoryRequest(appName, configuration, RequestType.DIRECTORY,
				dir.getCanonicalPath()) {

			@Override
			protected void onPostExecute(List<String> childs) {
				if (childs == null) {
					return;
				}
				viewDirectory(childs);
			}
		}.execute();
	}

	private void viewDirectory(List<String> childs) {
		directory.setAdapter(new ArrayAdapter<String>(activity,
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
								RequestType.DIRECTORY, canonicalPath) {

							@Override
							protected void onPostExecute(List<String> childs) {
								if (childs == null) {
									return;
								}
								viewDirectory(childs);
							}
						}.execute();
					} else if (dirEntry.endsWith(".sid")) {
						showSid(canonicalPath);
						tabHost.setCurrentTabByTag(SidTab.class.getSimpleName());
					} else {
						showMedia(canonicalPath);
					}
				} catch (IOException e) {
					Log.e(appName, e.getMessage(), e);
				}
			}

		});
	}

	public void viewDirectory(final File dir) {
		try {
			requestDirectory(dir);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	protected abstract void showMedia(String canonicalPath);
	protected abstract void showSid(String canonicalPath);

}
