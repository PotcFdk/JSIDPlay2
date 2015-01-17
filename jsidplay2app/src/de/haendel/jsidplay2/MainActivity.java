package de.haendel.jsidplay2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import de.haendel.jsidplay2.JSIDPlay2RESTRequest.RequestType;

public class MainActivity extends Activity {

	private String appName;
	private SidTab sidTab;
	private PlayList playList;
	private Configuration configuration;

	private TabHost tabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		appName = getApplication().getString(R.string.app_name);
		configuration = new Configuration();

		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();

		new GeneralTab(this, appName, configuration, tabHost);
		new SidsTab(this, appName, configuration, tabHost) {
			@Override
			protected SidTab getSidTab() {
				return sidTab;
			}
		};
		sidTab = new SidTab(this, appName, configuration, tabHost);
		playList = new PlayList(this, appName, configuration, tabHost) {
			@Override
			protected SidTab getSidTab() {
				return sidTab;
			}
		};
		new ConfigurationTab(this, appName, configuration, tabHost);

		loadFavorites(null);
	}

	public void onStop() {
		super.onStop();
		playList.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void asSid(View view) {
		new DownloadRequest(appName, configuration, RequestType.DOWNLOAD,
				sidTab.getCurrentTune()) {
			protected void onPostExecute(DataAndType music) {
				if (music == null) {
					return;
				}
				saveDownload(music);
			}
		}.execute();
	}

	public void loadFavorites(View view) {
		try {
			playList.load();
		} catch (UnsupportedEncodingException e) {
			Log.e(appName, e.getMessage(), e);
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

	public void removeFavorite(View view) {
		try {
			playList.remove();
			playList.save();
		} catch (UnsupportedEncodingException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	public void next(View view) {
		try {
			playList.play(playList.next());
		} catch (UnsupportedEncodingException e) {
			Log.e(appName, e.getMessage(), e);
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

	public void addToPlaylist(View view) {
		try {
			playList.add(sidTab.getCurrentTune());
			playList.save();
		} catch (IllegalArgumentException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (SecurityException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (URISyntaxException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	public void stop(View view) {
		playList.stop();
	}

	private void saveDownload(DataAndType music) {
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(music.uri, music.type);
		startActivity(intent);
	}

}
