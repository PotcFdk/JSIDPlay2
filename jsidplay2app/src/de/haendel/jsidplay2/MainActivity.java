package de.haendel.jsidplay2;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends TabActivity {

	private static final String HVSC_FILTER = ".*\\.(sid)$";
	private static final String CGSC_FILTER = ".*\\.(dat|mus|str)$";

	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	private static final String DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String CONVERT_URL = ROOT_URL + "/convert";
	private static final String DIRECTORY_URL = ROOT_URL + "/directory";
	private static final String LOAD_PHOTO_URL = ROOT_URL + "/photo";
	private static final String GET_TUNE_INFOS = ROOT_URL + "/info";

	private static final String DOWNLOAD_DIR = "Download";

	private String hostname, port, username, password;

	private TabHost mTabHost;

	private class DataAndType {
		private Uri uri;
		private String type;
	}

	private class LongRunningRequest extends AsyncTask<Void, Void, Object> {
		private String url;
		private int listViewId;
		private String filter;

		public LongRunningRequest(String url, int listViewId, String filter) {
			this.url = url;
			this.listViewId = listViewId;
			this.filter = filter;
			enableDisableUI(false, listViewId);
		}

		@Override
		protected Object doInBackground(Void... params) {
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();

				HttpParams httpParams = httpClient.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
				HttpConnectionParams.setSoTimeout(httpParams, 5000);

				httpClient.getCredentialsProvider().setCredentials(
						new AuthScope(hostname, AuthScope.ANY_PORT),
						new UsernamePasswordCredentials(username, password));

				URI myUri = new URI("http", null, hostname,
						Integer.valueOf(port), url, filter != null ? "filter="
								+ filter : null, null);

				Log.d(getApplication().getString(R.string.app_name),
						"HTTP-GET: " + myUri);

				HttpGet httpGet = new HttpGet(myUri);
				HttpContext localContext = new BasicHttpContext();
				HttpResponse response = httpClient.execute(httpGet,
						localContext);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpURLConnection.HTTP_OK) {
					HttpEntity entity = response.getEntity();
					if (url.startsWith(DIRECTORY_URL)) {
						return getDirectoryFromEntity(entity);
					} else if (url.startsWith(LOAD_PHOTO_URL)) {
						return getPhotoFromEntity(entity);
					} else if (url.startsWith(GET_TUNE_INFOS)) {
						return getTuneInfos(entity);
					} else {
						return getDownloadFromEntity(entity);
					}
				} else {
					Log.e(getApplication().getString(R.string.app_name), String
							.format("URL: '%s', HTTP status: '%d':",
									myUri.toString(), statusCode));
					return null;
				}
			} catch (IOException e) {
				Log.e(getApplication().getString(R.string.app_name),
						e.getMessage(), e);
				return null;
			} catch (URISyntaxException e) {
				Log.e(getApplication().getString(R.string.app_name),
						e.getMessage(), e);
				return null;
			}
		}

		private Object getTuneInfos(HttpEntity entity) throws IOException {
			InputStream in = entity.getContent();
			final StringBuffer out = new StringBuffer();
			int n = 1;
			while (n > 0) {
				byte[] b = new byte[4096];
				n = in.read(b);
				if (n > 0)
					out.append(new String(b, 0, n));
			}
			System.out.println(out);
			return out;
		}

		private String getString(String s2) {
			s2 = s2.replaceAll("[.]", "_");
			for (Field field : R.string.class.getDeclaredFields()) {
				if (field.getName().equals(s2)) {
					try {
						return MainActivity.this.getString(field.getInt(null));
					} catch (IllegalArgumentException e) {
						return "???";
					} catch (IllegalAccessException e) {
						return "???";
					}
				}
			}
			return "???";
		}

		protected byte[] getPhotoFromEntity(HttpEntity entity)
				throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			ByteArrayOutputStream s = new ByteArrayOutputStream();
			int n = 1;
			while (n > 0) {
				byte[] b = new byte[4096];
				n = in.read(b);
				if (n > 0)
					s.write(b, 0, n);
			}
			return s.toByteArray();
		}

		protected String[] getDirectoryFromEntity(HttpEntity entity)
				throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			StringBuffer out = new StringBuffer();
			int n = 1;
			while (n > 0) {
				byte[] b = new byte[4096];
				n = in.read(b);
				if (n > 0)
					out.append(new String(b, 0, n));
			}
			String line = out.substring(1, out.length() - 1);
			String[] split = splitJSONToken(line, ",");
			return split;
		}

		private String[] splitJSONToken(String line, String sep) {
			String otherThanQuote = " [^\"] ";
			String quotedString = String.format(" \" %s* \" ", otherThanQuote);
			String regex = String.format("(?x) " + // enable comments,
													// ignore white spaces
					sep + "                         " + // match a comma
					"(?=                       " + // start positive look
													// ahead
					"  (                       " + // start group 1
					"    %s*                   " + // match 'otherThanQuote'
													// zero or more times
					"    %s                    " + // match 'quotedString'
					"  )*                      " + // end group 1 and repeat
													// it zero or more times
					"  %s*                     " + // match 'otherThanQuote'
					"  $                       " + // match the end of the
													// string
					")                         ", // stop positive look
													// ahead
					otherThanQuote, quotedString, otherThanQuote);
			String[] split = line.split(regex, -1);
			return split;
		}

		protected DataAndType getDownloadFromEntity(HttpEntity entity)
				throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			long length = entity.getContentLength();

			File sdRootDir = Environment.getExternalStorageDirectory();
			File music = new File(new File(sdRootDir, DOWNLOAD_DIR), new File(
					url).getName());

			OutputStream out;
			byte[] b = new byte[4096];
			if (sdRootDir.canWrite()) {
				out = new BufferedOutputStream(new FileOutputStream(music));
				while (length > 0) {
					int n = in.read(b);
					if (n > 0) {
						out.write(b, 0, n);
						length -= n;
					}
				}
				out.close();
			}
			DataAndType dt = new DataAndType();
			dt.uri = Uri.fromFile(music);
			dt.type = entity.getContentType().getValue();
			return dt;
		}

		protected void onPostExecute(Object result) {
			if (result instanceof StringBuffer) {
				final StringBuffer out = (StringBuffer) result;
				runOnUiThread(new Runnable() {
					public void run() {
						List<Pair<String, String>> rows = new ArrayList<Pair<String, String>>();

						String line = out.substring(1, out.length() - 1);
						String[] split = splitJSONToken(line, ",");
						for (String s : split) {
							System.out.println(s);
							String[] split2 = splitJSONToken(s, ":");
							String name = null;
							String value = "";
							for (String s2 : split2) {
								String key = s2.substring(1, s2.length() - 1);
								if (name == null) {
									name = getString(key);
									key = name;
								} else {
									value = key;
								}
								System.out.println(s2);
							}
							if (!value.equals("")) {
								Pair<String, String> p = new Pair<String, String>(
										name, value);
								rows.add(p);
							}
						}
						Comparator<? super Pair<String, String>> rowC = new Comparator<Pair<String, String>>() {
							@Override
							public int compare(Pair<String, String> lhs,
									Pair<String, String> rhs) {
								return lhs.first.compareTo(rhs.first);
							}
						};
						Collections.sort(rows, rowC);
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
					}
				});
			} else if (result instanceof byte[]) {
				byte[] photo = (byte[]) result;
				ImageView image = (ImageView) findViewById(R.id.imageView1);
				Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0,
						photo.length);
				image.setImageBitmap(bitmap);
			} else if (result instanceof DataAndType) {
				// Open Download
				DataAndType music = (DataAndType) result;

				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(music.uri, music.type);
				startActivity(intent);
			} else if (result instanceof String[]) {
				// View Directory
				String[] childs = (String[]) result;

				ArrayList<String> list = new ArrayList<String>();
				for (String child : childs) {
					if (child.length() > 2) {
						list.add(child.substring(1, child.length() - 1));
					}
				}
				ListView listview = (ListView) findViewById(listViewId);
				listview.setAdapter(new ArrayAdapter<String>(MainActivity.this,
						android.R.layout.simple_list_item_1, list));
				listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent,
							final View view, int position, long id) {
						final String item = (String) parent
								.getItemAtPosition(position);
						File file = new File(item);
						try {
							if (item.endsWith(".mp3")) {
								new LongRunningRequest(DOWNLOAD_URL
										+ file.getCanonicalPath(), listViewId,
										filter).execute();
							} else if (item.indexOf(".") != -1) {

								TextView resource = (TextView) findViewById(R.id.resource);
								resource.setText(file.getCanonicalPath());
								mTabHost.getTabWidget().getChildTabViewAt(3)
										.setEnabled(true);
								new LongRunningRequest(LOAD_PHOTO_URL
										+ file.getCanonicalPath(), listViewId,
										null).execute();
								new LongRunningRequest(GET_TUNE_INFOS
										+ file.getCanonicalPath(), listViewId,
										null).execute();
								mTabHost.setCurrentTab(3);
							} else {
								new LongRunningRequest(DIRECTORY_URL
										+ file.getCanonicalPath(), listViewId,
										filter).execute();
							}
						} catch (IOException e) {
							Log.e(getApplication().getString(R.string.app_name),
									e.getMessage(), e);
						}
					}

				});
			}
			enableDisableUI(true, listViewId);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);

		mTabHost.addTab(mTabHost.newTabSpec("tab_test1").setIndicator("Conn")
				.setContent(R.id.general));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test2").setIndicator("HVSC")
				.setContent(R.id.hvsc));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test3").setIndicator("CGSC")
				.setContent(R.id.cgsc));
		TabSpec setContent = mTabHost.newTabSpec("tab_test4")
				.setIndicator("Tune").setContent(R.id.tune);
		mTabHost.addTab(setContent);

		mTabHost.setCurrentTab(0);
		mTabHost.getTabWidget().getChildTabViewAt(3).setEnabled(false);
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

			public void onTabChanged(String tabId) {
				ListView view;
				Log.d(getApplication().getString(R.string.app_name),
						"onTabChanged: tab number=" + mTabHost.getCurrentTab());

				switch (mTabHost.getCurrentTab()) {
				case 0:
					// do what you want when tab 0 is selected
					break;
				case 1:
					// do what you want when tab 1 is selected
					setHostnamePort();
					view = (ListView) findViewById(R.id.listView1);
					if (!"".equals(hostname) && view.getCount() == 0) {
						requestDirectory(new File("/C64Music"), view.getId(),
								HVSC_FILTER);
					}
					break;
				case 2:
					// do what you want when tab 2 is selected
					setHostnamePort();
					view = (ListView) findViewById(R.id.listView2);
					if (!"".equals(hostname) && view.getCount() == 0) {
						requestDirectory(new File("/CGSC"), view.getId(),
								CGSC_FILTER);
					}
					break;

				default:

					break;
				}
			}
		});

		// For retrieving string value from sharedPreference
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String hostname = preferences.getString("hostname", "haendel.ddns.net");
		String port = preferences.getString("port", "8080");
		String username = preferences.getString("username", "jsidplay2");
		String password = preferences.getString("password", "jsidplay2!");
		((EditText) findViewById(R.id.hostname)).setText(hostname);
		((EditText) findViewById(R.id.port)).setText(port);
		((EditText) findViewById(R.id.username)).setText(username);
		((EditText) findViewById(R.id.password)).setText(password);
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

	private void requestDirectory(File dir, int listViewId, String filter) {
		try {
			new LongRunningRequest(DIRECTORY_URL + dir.getCanonicalPath(),
					listViewId, filter).execute();
		} catch (UnsupportedEncodingException e) {
			Log.e(getApplication().getString(R.string.app_name),
					e.getMessage(), e);
		} catch (IOException e) {
			Log.e(getApplication().getString(R.string.app_name),
					e.getMessage(), e);
		}
	}

	private void setHostnamePort() {
		hostname = ((EditText) findViewById(R.id.hostname)).getText()
				.toString();
		port = ((EditText) findViewById(R.id.port)).getText().toString();
		username = ((EditText) findViewById(R.id.username)).getText()
				.toString();
		password = ((EditText) findViewById(R.id.password)).getText()
				.toString();
		// For storing string value in sharedPreference
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("hostname", hostname);
		editor.putString("port", port);
		editor.putString("username", username);
		editor.putString("password", password);
		editor.commit();
	}

	private void enableDisableUI(boolean enable, int listViewId) {
		if (listViewId != -1) {
			View view = (View) findViewById(listViewId);
			view.setClickable(enable);
			view.setEnabled(enable);
		}
	}

	public void download(View view) {
		TextView resource = (TextView) findViewById(R.id.resource);
		String tune = String.valueOf(resource.getText());
		new LongRunningRequest(DOWNLOAD_URL + tune, -1, null).execute();
	}

	public void mp3(View view) {
		TextView resource = (TextView) findViewById(R.id.resource);
		CharSequence tune = resource.getText();
		try {
			URI uri = new URI("http", username + ":" + password, hostname,
					Integer.valueOf(port), CONVERT_URL + tune, "", null);
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
}
