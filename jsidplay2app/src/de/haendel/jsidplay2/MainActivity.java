package de.haendel.jsidplay2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import de.haendel.jsidplay2.JSIDPlay2Service.JSIDPlay2Binder;
import de.haendel.jsidplay2.JSIDPlay2Service.PlayListEntry;
import de.haendel.jsidplay2.config.Configuration;
import de.haendel.jsidplay2.request.DataAndType;
import de.haendel.jsidplay2.request.DownloadRequest;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;
import de.haendel.jsidplay2.tab.ConfigurationTab;
import de.haendel.jsidplay2.tab.GeneralTab;
import de.haendel.jsidplay2.tab.PlayListTab;
import de.haendel.jsidplay2.tab.SidTab;
import de.haendel.jsidplay2.tab.SidsTab;

public class MainActivity extends Activity {

	private String appName;

	private Configuration configuration;
	private boolean randomized;

	private TabHost tabHost;
	private SidTab sidTab;
	private PlayListTab playListTab;

	private JSIDPlay2Service jsidplay2service;
	private Intent playIntent;

	// connect to the service
	final ServiceConnection jsidplay2Connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			JSIDPlay2Binder binder = (JSIDPlay2Binder) service;
			// get service
			jsidplay2service = binder.getService();
			// pass configuration
			jsidplay2service.setConfiguration(configuration);
			jsidplay2service.setRandomized(randomized);

			for (PlayListEntry entry : jsidplay2service.getPlayList()) {
				playListTab.addRow(entry);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		if (playIntent == null) {
			playIntent = new Intent(this, JSIDPlay2Service.class);
			bindService(playIntent, jsidplay2Connection,
					Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	}

	public void playSong(PlayListEntry entry) {
		jsidplay2service.playSong(entry);
	}

	public void setRandomized(boolean randomized) {
		this.randomized = randomized;
		if (jsidplay2service != null) {
			jsidplay2service.setRandomized(randomized);
		}
	}

	public void next(View view) {
		jsidplay2service.playNextSong();
	}

	public void stop(View view) {
		jsidplay2service.stop();
		stopService(playIntent);
	}

	@Override
	protected void onDestroy() {
		unbindService(jsidplay2Connection);
		stopService(playIntent);
		jsidplay2service = null;
		super.onDestroy();
	}

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
			protected void showSid(String cannonicalPath) {
				sidTab.requestSidDetails(cannonicalPath);
			}
		};
		sidTab = new SidTab(this, appName, configuration, tabHost);
		playListTab = new PlayListTab(this, appName, configuration, tabHost) {
			@Override
			protected void play(PlayListEntry entry) {
				if (entry == null) {
					return;
				}

				playSong(entry);
				sidTab.requestSidDetails(entry.getResource());
			}
		};
		new ConfigurationTab(this, appName, configuration, tabHost);

		tabHost.setCurrentTabByTag(GeneralTab.class.getSimpleName());
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
		switch (item.getItemId()) {
		case R.id.action_settings:
			unbindService(jsidplay2Connection);
			stopService(playIntent);
			jsidplay2service = null;
			System.exit(0);
			break;
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
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(music.getUri(), music.getType());
				startActivity(intent);
			}
		}.execute();
	}

	public void removeFavorite(View view) {
		try {
			jsidplay2service.removeLast();
			playListTab.removeLast();
		} catch (UnsupportedEncodingException e) {
			Log.e(appName, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	public void addToPlaylist(View view) {
		try {
			String resource = sidTab.getCurrentTune();
			PlayListEntry entry = jsidplay2service.add(resource);
			playListTab.addRow(entry);
			tabHost.setCurrentTabByTag(PlayListTab.class.getSimpleName());
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

}
