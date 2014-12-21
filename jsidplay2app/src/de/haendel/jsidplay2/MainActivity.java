package de.haendel.jsidplay2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {

	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	private static final String DIR_PARAM = "dir=";
	private static final String FILE_PARAM = "file=";
	private static final String FILTER_PARAM = "filter=";
	private static final String HVSC_PARAM = "hvsc=";

	private static final String DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String CONVERT_URL = ROOT_URL + "/convert";
	private static final String DIRECTORY_URL = ROOT_URL + "/directory";

	private static final String MUSIK_DIR = "/media/readyshare/Musik";
	private static final String C64MUSIC_DIR = "/home/ken/Downloads/C64Music";

	private static final String MP3SAVE_DIR = "Download";

	private String hostname, port;

	protected static String filter;
	static {
		try {
			filter = URLEncoder.encode(".*\\.(mp3|sid)$", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private class DataAndType {
		private Uri uri;
		private String type;
	}

	private class LongRunningRequest extends AsyncTask<Void, Void, Object> {
		private String url;
		private String name;

		public LongRunningRequest(String url, String name) {
			this.url = url;
			this.name = name;
			disableUI();
		}

		@Override
		protected Object doInBackground(Void... params) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet("http://" + hostname + ":" + port
					+ url);
			try {
				HttpResponse response = httpClient.execute(httpGet,
						localContext);
				HttpEntity entity = response.getEntity();
				if (url.startsWith(DIRECTORY_URL)) {
					return getDirectoryFromEntity(entity);
				} else {
					return getDownloadFromEntity(entity);
				}
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
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
			String otherThanQuote = " [^\"] ";
			String quotedString = String.format(" \" %s* \" ", otherThanQuote);
			String regex = String.format("(?x) " + // enable comments,
													// ignore white spaces
					",                         " + // match a comma
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
			return line.split(regex, -1);
		}

		protected DataAndType getDownloadFromEntity(HttpEntity entity)
				throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			long length = entity.getContentLength();

			File root = Environment.getExternalStorageDirectory();
			File music = new File(root, MP3SAVE_DIR + "/"
					+ getBaseNameNoExt(name) + ".mp3");
			OutputStream out;
			byte[] b = new byte[4096];
			if (root.canWrite()) {
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
			if (result instanceof DataAndType) {
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
					list.add(child.substring(1, child.length() - 1));
				}
				ListView listview = (ListView) findViewById(R.id.listView1);
				listview.setAdapter(new ArrayAdapter<String>(MainActivity.this,
						android.R.layout.simple_list_item_1, list));
				listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent,
							final View view, int position, long id) {
						final String item = (String) parent
								.getItemAtPosition(position);
						try {
							File file = new File(item);
							String encode = URLEncoder.encode(
									file.getCanonicalPath(), "UTF-8");
							if (item.endsWith(".mp3")) {
								new LongRunningRequest(DOWNLOAD_URL + "?"
										+ FILE_PARAM + encode, file.getName())
										.execute();
							} else if (item.endsWith(".sid")) {
								new LongRunningRequest(CONVERT_URL + "?"
										+ FILE_PARAM + encode + "&&"
										+ HVSC_PARAM + C64MUSIC_DIR, file
										.getName()).execute();
							} else {
								new LongRunningRequest(DIRECTORY_URL + "?"
										+ DIR_PARAM + encode + "&&"
										+ FILTER_PARAM + filter, file.getName())
										.execute();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				});
			}
			enableUI();
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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

	public void music(View view) {
		hostname = ((EditText) findViewById(R.id.hostname)).getText()
				.toString();
		port = ((EditText) findViewById(R.id.port)).getText().toString();
		try {
			File file = new File(MUSIK_DIR);
			String encode = URLEncoder.encode(file.getCanonicalPath(), "UTF-8");
			new LongRunningRequest(DIRECTORY_URL + "?" + DIR_PARAM + encode
					+ "&&" + FILTER_PARAM + filter, file.getName()).execute();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void c64music(View view) {
		try {
			File file = new File(C64MUSIC_DIR);
			String encode = URLEncoder.encode(file.getCanonicalPath(), "UTF-8");
			new LongRunningRequest(DIRECTORY_URL + "?" + DIR_PARAM + encode
					+ "&&" + FILTER_PARAM + filter, file.getName()).execute();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void disableUI() {
		Button b = (Button) findViewById(R.id.button1);
		b.setClickable(false);
		b.setEnabled(false);
		b = (Button) findViewById(R.id.button2);
		b.setClickable(false);
		b.setEnabled(false);
		ListView listview = (ListView) findViewById(R.id.listView1);
		listview.setClickable(false);
		listview.setEnabled(false);
	}

	private void enableUI() {
		Button b = (Button) findViewById(R.id.button1);
		b.setClickable(true);
		b.setEnabled(true);
		b = (Button) findViewById(R.id.button2);
		b.setClickable(true);
		b.setEnabled(true);
		ListView listview = (ListView) findViewById(R.id.listView1);
		listview.setClickable(true);
		listview.setEnabled(true);
	}

	private static final String getBaseNameNoExt(final String name) {
		int lastIndexOf = name.lastIndexOf('.');
		final String basename;
		if (lastIndexOf != -1) {
			basename = name.substring(0, lastIndexOf);
		} else {
			basename = name;
		}
		return basename;
	}

}
