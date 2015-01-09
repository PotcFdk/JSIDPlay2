package de.haendel.jsidplay2;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String SIDS_FILTER = ".*\\.(sid|dat|mus|str)$";

	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	static final String REST_DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String REST_CONVERT_URL = ROOT_URL + "/convert";
	static final String REST_DIRECTORY_URL = ROOT_URL + "/directory";
	static final String REST_LOAD_PHOTO_URL = ROOT_URL + "/photo";
	static final String REST_TUNE_INFOS = ROOT_URL + "/info";

	private static final String PAR_HOSTNAME = "hostname";
	private static final String PAR_PORT = "port";
	private static final String PAR_USERNAME = "username";
	private static final String PAR_PASSWORD = "password";
	private static final String PAR_EMULATION = "emulation";
	private static final String PAR_ENABLE_DATABASE = "enableDatabase";
	private static final String PAR_DEFAULT_PLAY_LENGTH = "defaultPlayLength";

	private static final String RESID = "RESID";
	private static final String RESIDFP = "RESIDFP";

	private static final String DEFAULT_HOSTNAME = "haendel.ddns.net";
	private static final String DEFAULT_PORT = "8080";
	private static final String DEFAULT_USERNAME = "jsidplay2";
	private static final String DEFAULT_PASSWORD = "jsidplay2!";

	private String appName;
	private TabHost mTabHost;
	private Connection connection = new Connection();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// For retrieving string value from sharedPreference
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		appName = getApplication().getString(R.string.app_name);
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec("tab_test1").setIndicator("Conn")
				.setContent(R.id.general));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test2").setIndicator("Music")
				.setContent(R.id.sids));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test4").setIndicator("Tune")
				.setContent(R.id.tune));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test5").setIndicator("Cfg")
				.setContent(R.id.settings));

		mTabHost.setCurrentTab(0);
		mTabHost.getTabWidget().getChildTabViewAt(2).setEnabled(false);
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

			public void onTabChanged(String tabId) {
				switch (mTabHost.getCurrentTab()) {
				case 0:
					// connection tab shown initially
					break;
				case 1:
					requestDirectory(new File("/"),
							(ListView) findViewById(R.id.listView1),
							SIDS_FILTER);
					break;

				default:
					// tune infos automatically switched to
					break;
				}
				Log.d(getApplication().getString(R.string.app_name),
						"onTabChanged: tab number=" + mTabHost.getCurrentTab());
			}
		});

		String[] array_spinner = new String[2];
		array_spinner[0] = RESID;
		array_spinner[1] = RESIDFP;
		Spinner s = (Spinner) findViewById(R.id.emulation);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, array_spinner);
		s.setAdapter(adapter);
		s.setSelection(preferences.getString(PAR_EMULATION, RESIDFP).equals(
				RESID) ? 0 : 1);

		((CheckBox) findViewById(R.id.songlength)).setChecked(Boolean
				.valueOf(preferences.getString(PAR_ENABLE_DATABASE, "false")));
		((EditText) findViewById(R.id.defaultLength)).setText(preferences
				.getString(PAR_DEFAULT_PLAY_LENGTH, "0"));

		((EditText) findViewById(R.id.hostname)).setText(preferences.getString(
				PAR_HOSTNAME, DEFAULT_HOSTNAME));
		((EditText) findViewById(R.id.port)).setText(preferences.getString(
				PAR_PORT, DEFAULT_PORT));
		((EditText) findViewById(R.id.username)).setText(preferences.getString(
				PAR_USERNAME, DEFAULT_USERNAME));
		((EditText) findViewById(R.id.password)).setText(preferences.getString(
				PAR_PASSWORD, DEFAULT_PASSWORD));

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

	private void requestDirectory(File dir, final ListView view,
			final String filter) {
		try {
			setHostnamePort();
			if (!"".equals(connection.getHostname()) && view.getCount() == 0) {
				enableDisableUI(false, view.getId());
				new DirectoryRequest(appName, connection, REST_DIRECTORY_URL
						+ dir.getCanonicalPath()) {

					@Override
					protected void onPostExecute(List<String> childs) {
						if (childs == null) {
							return;
						}
						// View Directory
						viewDirectory(childs, view.getId(), filter);
					}
				}.execute(filter);
			}
		} catch (UnsupportedEncodingException e) {
			Log.e(getApplication().getString(R.string.app_name),
					e.getMessage(), e);
		} catch (IOException e) {
			Log.e(getApplication().getString(R.string.app_name),
					e.getMessage(), e);
		}
	}

	private void setHostnamePort() {
		connection.setHostname(((EditText) findViewById(R.id.hostname))
				.getText().toString());
		connection.setPort(((EditText) findViewById(R.id.port)).getText()
				.toString());
		connection.setUsername(((EditText) findViewById(R.id.username))
				.getText().toString());
		connection.setPassword(((EditText) findViewById(R.id.password))
				.getText().toString());

		// For storing string value in sharedPreference
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PAR_HOSTNAME, connection.getHostname());
		editor.putString(PAR_PORT, connection.getPort());
		editor.putString(PAR_USERNAME, connection.getUsername());
		editor.putString(PAR_PASSWORD, connection.getPassword());
		editor.putString(PAR_EMULATION,
				((Spinner) findViewById(R.id.emulation)).getSelectedItem()
						.toString());
		editor.putString(PAR_ENABLE_DATABASE, Boolean
				.toString(((CheckBox) findViewById(R.id.songlength))
						.isChecked()));
		editor.putString(PAR_DEFAULT_PLAY_LENGTH,
				((EditText) findViewById(R.id.defaultLength)).getText()
						.toString());
		editor.commit();

	}

	void enableDisableUI(boolean enable, int listViewId) {
		if (listViewId != -1) {
			View view = (View) findViewById(listViewId);
			view.setClickable(enable);
			view.setEnabled(enable);
		}
	}

	public void download(View view) {
		TextView resource = (TextView) findViewById(R.id.resource);
		String tune = String.valueOf(resource.getText());
		enableDisableUI(false, -1);
		new DownloadRequest(appName, connection, REST_DOWNLOAD_URL + tune) {
			protected void onPostExecute(DataAndType music) {
				if (music == null) {
					return;
				}
				saveDownload(music, -1);
			}
		}.execute();
	}

	public void mp3(View view) {
		TextView resource = (TextView) findViewById(R.id.resource);
		CharSequence tune = resource.getText();
		try {
			Spinner s = (Spinner) findViewById(R.id.emulation);
			CheckBox box = (CheckBox) findViewById(R.id.songlength);
			EditText txt = (EditText) findViewById(R.id.defaultLength);

			String query = "";
			query += PAR_EMULATION + "=" + s.getSelectedItem().toString() + "&";
			query += PAR_ENABLE_DATABASE + "=" + box.isChecked() + "&";
			query += PAR_DEFAULT_PLAY_LENGTH + "=" + getNumber(txt);
			URI uri = new URI("http", connection.getUsername() + ":"
					+ connection.getPassword(), connection.getHostname(),
					Integer.valueOf(connection.getPort()), REST_CONVERT_URL
							+ tune, query, null);
			Uri myUri = Uri.parse(uri.toString());

			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(myUri, "audio/mpeg");
			startActivity(intent);
		} catch (NumberFormatException e) {
			Log.e(getApplication().getString(R.string.app_name),
					e.getMessage(), e);
		} catch (URISyntaxException e) {
			Log.e(getApplication().getString(R.string.app_name),
					e.getMessage(), e);
		}
	}

	private int getNumber(EditText txt) {
		try {
			return Integer.parseInt(txt.getText().toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void viewDirectory(List<String> childs, final int listViewId,
			final String filter) {
		ListView listview = (ListView) findViewById(listViewId);
		listview.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_list_item_1, childs));
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				final String item = (String) parent.getItemAtPosition(position);
				File file = new File(item);
				try {
					if (item.endsWith("/")) {
						new DirectoryRequest(appName, connection,
								REST_DIRECTORY_URL + file.getCanonicalPath()) {

							@Override
							protected void onPostExecute(List<String> childs) {
								if (childs == null) {
									return;
								}
								viewDirectory(childs, listViewId, filter);
							}
						}.execute(filter);
					} else {
						TextView resource = (TextView) findViewById(R.id.resource);
						resource.setText(file.getCanonicalPath());
						new PhotoRequest(appName, connection,
								REST_LOAD_PHOTO_URL + file.getCanonicalPath()) {
							@Override
							protected void onPostExecute(byte[] photo) {
								if (photo == null) {
									return;
								}
								viewPhoto(photo, listViewId);
							}
						}.execute();
						new TuneInfoRequest(appName, connection,
								REST_TUNE_INFOS + file.getCanonicalPath()) {
							protected String getString(String key) {
								key = key.replaceAll("[.]", "_");
								for (Field field : R.string.class
										.getDeclaredFields()) {
									if (field.getName().equals(key)) {
										try {
											return MainActivity.this
													.getString(field
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
								viewTuneInfos(out, listViewId);
							}
						}.execute();
						mTabHost.getTabWidget().getChildTabViewAt(2)
								.setEnabled(true);
						mTabHost.setCurrentTab(2);
					}
				} catch (IOException e) {
					Log.e(getApplication().getString(R.string.app_name),
							e.getMessage(), e);
				}
			}

		});
		enableDisableUI(true, listViewId);
	}

	private void viewPhoto(byte[] photo, final int listViewId) {
		ImageView image = (ImageView) findViewById(R.id.imageView1);
		Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
		image.setImageBitmap(bitmap);
		enableDisableUI(true, listViewId);
	}

	private void viewTuneInfos(List<Pair<String, String>> rows,
			final int listViewId) {
		TableLayout table = (TableLayout) findViewById(R.id.table);
		table.removeAllViews();
		for (Pair<String, String> r : rows) {
			TableRow tr = new TableRow(MainActivity.this);
			tr.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.MATCH_PARENT));

			/* Create a Button to be the row-content. */
			TextView b = new TextView(MainActivity.this);
			b.setText(r.first);
			b.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			/* Add Button to row. */
			tr.addView(b);

			/* Create a Button to be the row-content. */
			b = new TextView(MainActivity.this);
			b.setText(r.second);
			b.setSingleLine(false);
			b.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			/* Add Button to row. */
			tr.addView(b);
			/* Add row to TableLayout. */
			// tr.setBackgroundResource(R.drawable.sf_gradient_03);
			table.addView(tr, new TableLayout.LayoutParams(
					TableLayout.LayoutParams.MATCH_PARENT,
					TableLayout.LayoutParams.WRAP_CONTENT));
		}
		enableDisableUI(true, listViewId);
	}

	private void saveDownload(DataAndType music, final int listViewId) {
		// Open Download
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(music.uri, music.type);
		startActivity(intent);
		enableDisableUI(true, listViewId);
	}

}
