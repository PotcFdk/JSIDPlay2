package de.haendel.jsidplay2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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

	private static final String TUNE_FILTER = ".*\\.(sid|dat|mus|str)$";

	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	static final String REST_DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String REST_CONVERT_URL = ROOT_URL + "/convert";
	static final String REST_DIRECTORY_URL = ROOT_URL + "/directory";
	static final String REST_PHOTO_URL = ROOT_URL + "/photo";
	static final String REST_INFO = ROOT_URL + "/info";
	static final String REST_FILTERS_URL = ROOT_URL + "/filters";

	private static final String PAR_HOSTNAME = "hostname";
	private static final String PAR_PORT = "port";
	private static final String PAR_USERNAME = "username";
	private static final String PAR_PASSWORD = "password";
	private static final String PAR_EMULATION = "emulation";
	private static final String PAR_ENABLE_DATABASE = "enableDatabase";
	private static final String PAR_DEFAULT_PLAY_LENGTH = "defaultPlayLength";
	private static final String PAR_DEFAULT_MODEL = "defaultSidModel";
	private static final String PAR_SINGLE_SONG = "single";
	private static final String PAR_LOOP = "loop";
	private static final String PAR_FILTER_6581 = "filter6581";
	private static final String PAR_STEREO_FILTER_6581 = "stereoFilter6581";
	private static final String PAR_FILTER_8580 = "filter8580";
	private static final String PAR_STEREO_FILTER_8580 = "stereoFilter8580";
	private static final String PAR_RESIDFP_FILTER_6581 = "reSIDfpFilter6581";
	private static final String PAR_RESIDFP_STEREO_FILTER_6581 = "reSIDfpStereoFilter6581";
	private static final String PAR_RESIDFP_FILTER_8580 = "reSIDfpFilter8580";
	private static final String PAR_RESIDFP_STEREO_FILTER_8580 = "reSIDfpStereoFilter8580";
	private static final String PAR_DIGI_BOOSTED_8580 = "digiBoosted8580";

	private static final String RESID = "RESID";
	private static final String RESIDFP = "RESIDFP";

	private static final String MOS6581 = "MOS6581";
	private static final String MOS8580 = "MOS8580";

	private static final String DEFAULT_HOSTNAME = "haendel.ddns.net";
	private static final String DEFAULT_PORT = "8080";
	private static final String DEFAULT_USERNAME = "jsidplay2";
	private static final String DEFAULT_PASSWORD = "jsidplay2!";
	private static final String DEFAULT_DEFAULT_PLAY_LENGTH = "0";
	private static final String DEFAULT_ENABLE_DATABASE = Boolean.FALSE
			.toString();
	private static final String DEFAULT_SINGLE_SONG = Boolean.FALSE.toString();
	private static final String DEFAULT_LOOP = Boolean.FALSE.toString();
	private static final String DEFAULT_DIGI_BOOSTED_8580 = Boolean.FALSE
			.toString();
	private static final String DEFAULT_FILTER_6581 = "FilterAverage6581";
	private static final String DEFAULT_FILTER_8580 = "FilterAverage8580";
	private static final String DEFAULT_RESIDFP_FILTER_6581 = "FilterAlankila6581R4AR_3789";
	private static final String DEFAULT_RESIDFP_FILTER_8580 = "FilterTrurl8580R5_3691";

	private static final String PREFIX_FILTER_6581 = "RESID_MOS6581_";
	private static final String PREFIX_FILTER_8580 = "RESID_MOS8580_";
	private static final String PREFIX_RESIDFP_FILTER_6581 = "RESIDFP_MOS6581_";
	private static final String PREFIX_RESIDFP_FILTER_8580 = "RESIDFP_MOS8580_";

	// private static final int CONNECTION_TAB_IDX = 0;
	private static final int SIDS_TAB_IDX = 1;
	private static final int TUNE_TAB_IDX = 2;
	private static final int SETTINGS_TAB_IDX = 3;

	private String appName;
	private Connection connection = new Connection();
	private SharedPreferences preferences;

	private TabHost tabHost;
	private EditText hostname, port, username, password, defaultLength;
	private ListView directory;
	private TextView resource;
	private ImageView image;
	private TableLayout table;
	private CheckBox enableDatabase, singleSong, loop, digiBoosted8580;
	private Spinner emulation, defaultModel;

	private Spinner filter6581, filter8580, reSIDfpFilter6581,
			reSIDfpFilter8580;
	private TextView filter6581txt, filter8580txt, reSIDfpFilter6581txt,
			reSIDfpFilter8580txt;

	private Spinner stereoFilter6581, stereoFilter8580,
			reSIDfpStereoFilter6581, reSIDfpStereoFilter8580;
	private TextView stereoFilter6581txt, stereoFilter8580txt,
			reSIDfpStereoFilter6581txt, reSIDfpStereoFilter8580txt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		appName = getApplication().getString(R.string.app_name);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		hostname = (EditText) findViewById(R.id.hostname);
		port = (EditText) findViewById(R.id.port);
		username = (EditText) findViewById(R.id.username);
		password = (EditText) findViewById(R.id.password);
		directory = (ListView) findViewById(R.id.directory);
		resource = (TextView) findViewById(R.id.resource);
		image = (ImageView) findViewById(R.id.image);
		table = (TableLayout) findViewById(R.id.table);
		defaultLength = (EditText) findViewById(R.id.defaultLength);
		enableDatabase = (CheckBox) findViewById(R.id.enableDatabase);
		singleSong = (CheckBox) findViewById(R.id.singleSong);
		loop = (CheckBox) findViewById(R.id.loop);
		digiBoosted8580 = (CheckBox) findViewById(R.id.digiBoosted8580);
		emulation = (Spinner) findViewById(R.id.emulation);
		defaultModel = (Spinner) findViewById(R.id.defaultModel);

		filter6581 = (Spinner) findViewById(R.id.filter6581);
		filter6581txt = (TextView) findViewById(R.id.filter6581txt);
		filter8580 = (Spinner) findViewById(R.id.filter8580);
		filter8580txt = (TextView) findViewById(R.id.filter8580txt);
		reSIDfpFilter6581 = (Spinner) findViewById(R.id.reSIDfpFilter6581);
		reSIDfpFilter6581txt = (TextView) findViewById(R.id.reSIDfpFilter6581txt);
		reSIDfpFilter8580 = (Spinner) findViewById(R.id.reSIDfpFilter8580);
		reSIDfpFilter8580txt = (TextView) findViewById(R.id.reSIDfpFilter8580txt);

		stereoFilter6581 = (Spinner) findViewById(R.id.stereoFilter6581);
		stereoFilter6581txt = (TextView) findViewById(R.id.stereoFilter6581txt);
		stereoFilter8580 = (Spinner) findViewById(R.id.stereoFilter8580);
		stereoFilter8580txt = (TextView) findViewById(R.id.stereoFilter8580txt);
		reSIDfpStereoFilter6581 = (Spinner) findViewById(R.id.reSIDfpStereoFilter6581);
		reSIDfpStereoFilter6581txt = (TextView) findViewById(R.id.reSIDfpStereoFilter6581txt);
		reSIDfpStereoFilter8580 = (Spinner) findViewById(R.id.reSIDfpStereoFilter8580);
		reSIDfpStereoFilter8580txt = (TextView) findViewById(R.id.reSIDfpStereoFilter8580txt);

		setupEditText(hostname, PAR_HOSTNAME, DEFAULT_HOSTNAME);
		setupEditText(port, PAR_PORT, DEFAULT_PORT);
		setupEditText(username, PAR_USERNAME, DEFAULT_USERNAME);
		setupEditText(password, PAR_PASSWORD, DEFAULT_PASSWORD);
		setupEditText(defaultLength, PAR_DEFAULT_PLAY_LENGTH,
				DEFAULT_DEFAULT_PLAY_LENGTH);

		setupCheckBox(enableDatabase, PAR_ENABLE_DATABASE,
				DEFAULT_ENABLE_DATABASE);
		setupCheckBox(singleSong, PAR_SINGLE_SONG, DEFAULT_SINGLE_SONG);
		setupCheckBox(loop, PAR_LOOP, DEFAULT_LOOP);
		setupCheckBox(digiBoosted8580, PAR_DIGI_BOOSTED_8580,
				DEFAULT_DIGI_BOOSTED_8580);

		setupSpinner(emulation, new String[] { RESID, RESIDFP }, PAR_EMULATION,
				RESIDFP);
		setupSpinner(defaultModel, new String[] { MOS6581, MOS8580 },
				PAR_DEFAULT_MODEL, MOS6581);

		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("tab_1")
				.setIndicator(getString(R.string.tab_connection))
				.setContent(R.id.general));
		tabHost.addTab(tabHost.newTabSpec("tab_2")
				.setIndicator(getString(R.string.tab_sids))
				.setContent(R.id.sids));
		tabHost.addTab(tabHost.newTabSpec("tab_4")
				.setIndicator(getString(R.string.tab_tune))
				.setContent(R.id.tune));
		tabHost.addTab(tabHost.newTabSpec("tab_5")
				.setIndicator(getString(R.string.tab_cfg))
				.setContent(R.id.settings));

		tabHost.getTabWidget().getChildTabViewAt(TUNE_TAB_IDX)
				.setEnabled(false);
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {

			public void onTabChanged(String tabId) {
				switch (tabHost.getCurrentTab()) {
				case SIDS_TAB_IDX:
				case SETTINGS_TAB_IDX:
					if (directory.getCount() == 0) {
						requestFilters();
						requestDirectory(new File("/"), TUNE_FILTER);
					}
					break;

				default:
					break;
				}
			}
		});
	}

	private final void setupEditText(final EditText editText,
			final String parName, final String defaultValue) {
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String newValue = editText.getText().toString();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(parName, newValue);
				editor.commit();
				setConnetionParameter(parName, newValue);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});
		editText.setText(preferences.getString(parName, defaultValue));
	}

	private void setupCheckBox(final CheckBox checkBox, final String parName,
			String defaultValue) {
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(parName,
						Boolean.toString(checkBox.isChecked()));
				editor.commit();
			}
		});
		checkBox.setChecked(Boolean.valueOf(preferences.getString(parName,
				defaultValue)));
	}

	private void setupSpinner(final Spinner spinner, final String[] items,
			final String parName, String defaultValue) {
		spinner.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, items));
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(parName, spinner.getSelectedItem().toString());
				editor.commit();
				spinnerUpdated(spinner, parName);
			}

			public void onNothingSelected(android.widget.AdapterView<?> parent) {
			}
		});
		String value = preferences.getString(parName, defaultValue);
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(value)) {
				spinner.setSelection(i);
				break;
			}
		}
	}

	private void spinnerUpdated(final Spinner spinner, final String parName) {
		if (PAR_EMULATION.equals(parName)) {
			updateFiltersVisibility(spinner);
		}
	}

	private void setConnetionParameter(String parName, String newValue) {
		if (PAR_HOSTNAME.equals(parName)) {
			connection.setHostname(newValue);
		} else if (PAR_PORT.equals(parName)) {
			connection.setPort(newValue);
		} else if (PAR_USERNAME.equals(parName)) {
			connection.setUsername(newValue);
		} else if (PAR_PASSWORD.equals(parName)) {
			connection.setPassword(newValue);
		}
	}

	private void updateFiltersVisibility(Spinner spinner) {
		Object selectedItem = spinner.getSelectedItem();

		boolean isReSid = selectedItem.equals("RESID");
		updateFiltersVisibility(new View[] { filter6581txt, filter6581,
				filter8580txt, filter8580 }, isReSid);
		updateFiltersVisibility(new View[] { stereoFilter6581txt,
				stereoFilter6581, stereoFilter8580txt, stereoFilter8580 },
				isReSid);

		boolean isReSidFp = selectedItem.equals("RESIDFP");
		updateFiltersVisibility(new View[] { reSIDfpFilter6581txt,
				reSIDfpFilter6581, reSIDfpFilter8580txt, reSIDfpFilter8580 },
				isReSidFp);
		updateFiltersVisibility(new View[] { reSIDfpStereoFilter6581txt,
				reSIDfpStereoFilter6581, reSIDfpStereoFilter8580txt,
				reSIDfpStereoFilter8580 }, isReSidFp);

	}

	private void updateFiltersVisibility(View[] views, boolean visible) {
		for (int i = 0; i < views.length; i++) {
			View view = views[i];
			view.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
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

	private void requestDirectory(final File dir, final String filter) {
		try {
			new DirectoryRequest(appName, connection, REST_DIRECTORY_URL
					+ dir.getCanonicalPath()) {

				@Override
				protected void onPostExecute(List<String> childs) {
					if (childs == null) {
						return;
					}
					viewDirectory(childs, filter);
				}
			}.execute(filter);
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	private void requestFilters() {
		new FiltersRequest(appName, connection, REST_FILTERS_URL) {
			@Override
			protected void onPostExecute(List<String> filters) {
				if (filters == null) {
					return;
				}
				List<String> filter6581List = determineFilterList(filters,
						PREFIX_FILTER_6581);
				List<String> filter8580List = determineFilterList(filters,
						PREFIX_FILTER_8580);
				List<String> reSidFpFilter6581List = determineFilterList(
						filters, PREFIX_RESIDFP_FILTER_6581);
				List<String> reSidFpFilter8580List = determineFilterList(
						filters, PREFIX_RESIDFP_FILTER_8580);

				setupSpinner(filter6581, filter6581List.toArray(new String[0]),
						PAR_FILTER_6581, preferences.getString(PAR_FILTER_6581,
								DEFAULT_FILTER_6581));
				setupSpinner(filter8580, filter8580List.toArray(new String[0]),
						PAR_FILTER_8580, preferences.getString(PAR_FILTER_8580,
								DEFAULT_FILTER_8580));
				setupSpinner(reSIDfpFilter6581,
						reSidFpFilter6581List.toArray(new String[0]),
						PAR_RESIDFP_FILTER_6581, preferences.getString(
								PAR_RESIDFP_FILTER_6581,
								DEFAULT_RESIDFP_FILTER_6581));
				setupSpinner(reSIDfpFilter8580,
						reSidFpFilter8580List.toArray(new String[0]),
						PAR_RESIDFP_FILTER_8580, preferences.getString(
								PAR_RESIDFP_FILTER_8580,
								DEFAULT_RESIDFP_FILTER_8580));

				setupSpinner(stereoFilter6581,
						filter6581List.toArray(new String[0]),
						PAR_STEREO_FILTER_6581, preferences.getString(
								PAR_STEREO_FILTER_6581, DEFAULT_FILTER_6581));
				setupSpinner(stereoFilter8580,
						filter8580List.toArray(new String[0]),
						PAR_STEREO_FILTER_8580, preferences.getString(
								PAR_STEREO_FILTER_8580, DEFAULT_FILTER_8580));
				setupSpinner(reSIDfpStereoFilter6581,
						reSidFpFilter6581List.toArray(new String[0]),
						PAR_RESIDFP_STEREO_FILTER_6581, preferences.getString(
								PAR_RESIDFP_STEREO_FILTER_6581,
								DEFAULT_RESIDFP_FILTER_6581));
				setupSpinner(reSIDfpStereoFilter8580,
						reSidFpFilter8580List.toArray(new String[0]),
						PAR_RESIDFP_STEREO_FILTER_8580, preferences.getString(
								PAR_RESIDFP_STEREO_FILTER_8580,
								DEFAULT_RESIDFP_FILTER_8580));
			}
		}.execute();
	}

	private List<String> determineFilterList(List<String> filters, String prefix) {
		List<String> result = new ArrayList<String>();
		for (String filter : filters) {
			if (filter.startsWith(prefix)) {
				result.add(filter.substring(prefix.length()));
			}
		}
		return result;
	}

	public void download(View view) {
		new DownloadRequest(appName, connection, REST_DOWNLOAD_URL
				+ resource.getText()) {
			protected void onPostExecute(DataAndType music) {
				if (music == null) {
					return;
				}
				saveDownload(music);
			}
		}.execute();
	}

	public void mp3(View view) {
		try {
			StringBuilder query = new StringBuilder();
			query.append(PAR_EMULATION + "=" + emulation.getSelectedItem()
					+ "&");
			query.append(PAR_ENABLE_DATABASE + "=" + enableDatabase.isChecked()
					+ "&");
			query.append(PAR_DEFAULT_PLAY_LENGTH + "="
					+ getNumber(defaultLength.getText().toString()) + "&");
			query.append(PAR_DEFAULT_MODEL + "="
					+ defaultModel.getSelectedItem() + "&");
			query.append(PAR_SINGLE_SONG + "=" + singleSong.isChecked() + "&");
			query.append(PAR_LOOP + "=" + loop.isChecked() + "&");

			query.append(PAR_FILTER_6581 + "=" + filter6581.getSelectedItem()
					+ "&");
			query.append(PAR_FILTER_8580 + "=" + filter8580.getSelectedItem()
					+ "&");
			query.append(PAR_RESIDFP_FILTER_6581 + "="
					+ reSIDfpFilter6581.getSelectedItem() + "&");
			query.append(PAR_RESIDFP_FILTER_8580 + "="
					+ reSIDfpFilter8580.getSelectedItem() + "&");

			query.append(PAR_STEREO_FILTER_6581 + "="
					+ stereoFilter6581.getSelectedItem() + "&");
			query.append(PAR_STEREO_FILTER_8580 + "="
					+ stereoFilter8580.getSelectedItem() + "&");
			query.append(PAR_RESIDFP_STEREO_FILTER_6581 + "="
					+ reSIDfpStereoFilter6581.getSelectedItem() + "&");
			query.append(PAR_RESIDFP_STEREO_FILTER_8580 + "="
					+ reSIDfpStereoFilter8580.getSelectedItem() + "&");
			query.append(PAR_DIGI_BOOSTED_8580 + "="
					+ digiBoosted8580.isChecked());

			URI uri = new URI("http", connection.getUsername() + ":"
					+ connection.getPassword(), connection.getHostname(),
					getNumber(connection.getPort()), REST_CONVERT_URL
							+ resource.getText(), query.toString(), null);

			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(uri.toString()), "audio/mpeg");
			startActivity(intent);
		} catch (URISyntaxException e) {
			Log.e(appName, e.getMessage(), e);
		}
	}

	private int getNumber(String txt) {
		try {
			return Integer.parseInt(txt);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void viewDirectory(List<String> childs, final String filter) {
		directory.setAdapter(new ArrayAdapter<String>(this,
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
						new DirectoryRequest(appName, connection,
								REST_DIRECTORY_URL + file.getCanonicalPath()) {

							@Override
							protected void onPostExecute(List<String> childs) {
								if (childs == null) {
									return;
								}
								viewDirectory(childs, filter);
							}
						}.execute(filter);
					} else {
						resource.setText(file.getCanonicalPath());
						new PhotoRequest(appName, connection, REST_PHOTO_URL
								+ file.getCanonicalPath()) {
							@Override
							protected void onPostExecute(byte[] photo) {
								if (photo == null) {
									return;
								}
								viewPhoto(photo);
							}
						}.execute();
						new TuneInfoRequest(appName, connection, REST_INFO
								+ file.getCanonicalPath()) {
							public String getString(String key) {
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
								viewTuneInfos(out);
							}
						}.execute();
						tabHost.getTabWidget().getChildTabViewAt(TUNE_TAB_IDX)
								.setEnabled(true);
						tabHost.setCurrentTab(TUNE_TAB_IDX);
					}
				} catch (IOException e) {
					Log.e(appName, e.getMessage(), e);
				}
			}

		});
	}

	private void viewPhoto(byte[] photo) {
		Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
		image.setImageBitmap(bitmap);
	}

	private void viewTuneInfos(List<Pair<String, String>> rows) {
		table.removeAllViews();
		for (Pair<String, String> r : rows) {
			TableRow tr = new TableRow(this);
			tr.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.MATCH_PARENT));

			TextView b = new TextView(this);
			b.setText(r.first);
			b.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			tr.addView(b);

			b = new TextView(this);
			b.setText(r.second);
			b.setSingleLine(false);
			b.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			tr.addView(b);
			table.addView(tr, new TableLayout.LayoutParams(
					TableLayout.LayoutParams.MATCH_PARENT,
					TableLayout.LayoutParams.WRAP_CONTENT));
		}
	}

	private void saveDownload(DataAndType music) {
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(music.uri, music.type);
		startActivity(intent);
	}

}
