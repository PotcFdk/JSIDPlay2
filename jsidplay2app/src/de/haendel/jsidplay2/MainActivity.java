package de.haendel.jsidplay2;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
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

	private static final String SIDS_FILTER = ".*\\.(sid|dat|mus|str)$";

	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	static final String REST_DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String REST_CONVERT_URL = ROOT_URL + "/convert";
	static final String REST_DIRECTORY_URL = ROOT_URL + "/directory";
	static final String REST_LOAD_PHOTO_URL = ROOT_URL + "/photo";
	static final String REST_TUNE_INFOS = ROOT_URL + "/info";
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
	private static final String PAR_FILTERRESID6581 = "filter6581";
	private static final String PAR_STEREO_FILTERRESID6581 = "stereoFilter6581";
	private static final String PAR_FILTERRESID8580 = "filter8580";
	private static final String PAR_STEREO_FILTERRESID8580 = "stereoFilter8580";
	private static final String PAR_FILTERRESIDfp6581 = "reSIDfpFilter6581";
	private static final String PAR_STEREO_FILTERRESIDfp6581 = "reSIDfpStereoFilter6581";
	private static final String PAR_FILTERRESIDfp8580 = "reSIDfpFilter8580";
	private static final String PAR_STEREO_FILTERRESIDfp8580 = "reSIDfpStereoFilter8580";

	private static final String RESID = "RESID";
	private static final String RESIDFP = "RESIDFP";

	private static final String MOS6581 = "MOS6581";
	private static final String MOS8580 = "MOS8580";

	private static final String DEFAULT_HOSTNAME = "haendel.ddns.net";
	private static final String DEFAULT_PORT = "8080";
	private static final String DEFAULT_USERNAME = "jsidplay2";
	private static final String DEFAULT_PASSWORD = "jsidplay2!";
	private static final String DEFAULT_FILTERRESID6581 = "FilterAverage6581";
	private static final String DEFAULT_FILTERRESID8580 = "FilterAverage8580";
	private static final String DEFAULT_FILTERRESIDFP6581 = "FilterAlankila6581R4AR_3789";
	private static final String DEFAULT_FILTERRESIDFP8580 = "FilterTrurl8580R5_3691";

	private String appName;
	private TabHost mTabHost;
	private Connection connection = new Connection();

	private SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// For retrieving string value from sharedPreference
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

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
				case 3:
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

		EditText hostname = (EditText) findViewById(R.id.hostname);
		hostname.setText(preferences.getString(PAR_HOSTNAME, DEFAULT_HOSTNAME));
		hostname.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_HOSTNAME, connection.getHostname());
				editor.commit();
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
		EditText port = (EditText) findViewById(R.id.port);
		port.setText(preferences.getString(PAR_PORT, DEFAULT_PORT));
		port.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_PORT, connection.getPort());
				editor.commit();
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

		EditText username = (EditText) findViewById(R.id.username);
		username.setText(preferences.getString(PAR_USERNAME, DEFAULT_USERNAME));
		username.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_USERNAME, connection.getUsername());
				editor.commit();
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

		EditText password = (EditText) findViewById(R.id.password);
		password.setText(preferences.getString(PAR_PASSWORD, DEFAULT_PASSWORD));
		password.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_PASSWORD, connection.getPassword());
				editor.commit();
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

		final Spinner emulation = (Spinner) findViewById(R.id.emulation);
		emulation.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, new String[] { RESID,
						RESIDFP }));
		emulation.setSelection(preferences.getString(PAR_EMULATION, RESIDFP)
				.equals(RESID) ? 0 : 1);
		emulation.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_EMULATION,
						((Spinner) findViewById(R.id.emulation))
								.getSelectedItem().toString());
				editor.commit();
				setFiltersVisibility(emulation);
			}

			public void onNothingSelected(android.widget.AdapterView<?> parent) {
			};
		});

		setFiltersVisibility(emulation);

		Spinner defaultModel = (Spinner) findViewById(R.id.defaultModel);
		defaultModel.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, new String[] { MOS6581,
						MOS8580 }));
		defaultModel.setSelection(preferences.getString(PAR_DEFAULT_MODEL,
				MOS6581).equals(MOS6581) ? 0 : 1);
		defaultModel.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_DEFAULT_MODEL,
						((Spinner) findViewById(R.id.defaultModel))
								.getSelectedItem().toString());
				editor.commit();
			}

			public void onNothingSelected(android.widget.AdapterView<?> parent) {
			};
		});

		CheckBox songLength = (CheckBox) findViewById(R.id.songlength);
		songLength.setChecked(Boolean.valueOf(preferences.getString(
				PAR_ENABLE_DATABASE, "false")));
		songLength.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_ENABLE_DATABASE, Boolean
						.toString(((CheckBox) findViewById(R.id.songlength))
								.isChecked()));
				editor.commit();
			}
		});
		EditText defaultLength = (EditText) findViewById(R.id.defaultLength);
		defaultLength.setText(preferences.getString(PAR_DEFAULT_PLAY_LENGTH,
				"0"));
		defaultLength.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_DEFAULT_PLAY_LENGTH,
						((EditText) findViewById(R.id.defaultLength)).getText()
								.toString());
				editor.commit();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		CheckBox singleSong = (CheckBox) findViewById(R.id.singleSong);
		singleSong.setChecked(Boolean.valueOf(preferences.getString(
				PAR_SINGLE_SONG, "false")));
		singleSong.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_SINGLE_SONG, Boolean
						.toString(((CheckBox) findViewById(R.id.singleSong))
								.isChecked()));
				editor.commit();
			}
		});

		CheckBox loop = (CheckBox) findViewById(R.id.loop);
		loop.setChecked(Boolean.valueOf(preferences
				.getString(PAR_LOOP, "false")));
		loop.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_LOOP, Boolean
						.toString(((CheckBox) findViewById(R.id.loop))
								.isChecked()));
				editor.commit();
			}
		});
	}

	private void setFiltersVisibility(Spinner emulation) {
		TextView filtersRESID6581txt = (TextView) findViewById(R.id.filtersRESID6581txt);
		Spinner filtersRESID6581 = (Spinner) findViewById(R.id.filtersRESID6581);
		TextView filtersRESID8580txt = (TextView) findViewById(R.id.filtersRESID8580txt);
		Spinner filtersRESID8580 = (Spinner) findViewById(R.id.filtersRESID8580);
		filtersRESID6581txt.setVisibility(emulation.getSelectedItem().equals(
				"RESID") ? View.VISIBLE : View.GONE);
		filtersRESID6581.setVisibility(emulation.getSelectedItem().equals(
				"RESID") ? View.VISIBLE : View.GONE);
		filtersRESID8580txt.setVisibility(emulation.getSelectedItem().equals(
				"RESID") ? View.VISIBLE : View.GONE);
		filtersRESID8580.setVisibility(emulation.getSelectedItem().equals(
				"RESID") ? View.VISIBLE : View.GONE);
		TextView filtersRESIDfp6581txt = (TextView) findViewById(R.id.filtersRESIDfp6581txt);
		Spinner filtersRESIDfp6581 = (Spinner) findViewById(R.id.filtersRESIDfp6581);
		TextView filtersRESIDfp8580txt = (TextView) findViewById(R.id.filtersRESIDfp8580txt);
		Spinner filtersRESIDfp8580 = (Spinner) findViewById(R.id.filtersRESIDfp8580);
		filtersRESIDfp6581txt.setVisibility(emulation.getSelectedItem().equals(
				"RESIDFP") ? View.VISIBLE : View.GONE);
		filtersRESIDfp6581.setVisibility(emulation.getSelectedItem().equals(
				"RESIDFP") ? View.VISIBLE : View.GONE);
		filtersRESIDfp8580txt.setVisibility(emulation.getSelectedItem().equals(
				"RESIDFP") ? View.VISIBLE : View.GONE);
		filtersRESIDfp8580.setVisibility(emulation.getSelectedItem().equals(
				"RESIDFP") ? View.VISIBLE : View.GONE);

		TextView stereoFiltersRESID6581txt = (TextView) findViewById(R.id.stereoFiltersRESID6581txt);
		Spinner stereoFiltersRESID6581 = (Spinner) findViewById(R.id.stereoFiltersRESID6581);
		TextView stereoFiltersRESID8580txt = (TextView) findViewById(R.id.stereoFiltersRESID8580txt);
		Spinner stereoFiltersRESID8580 = (Spinner) findViewById(R.id.stereoFiltersRESID8580);
		stereoFiltersRESID6581txt.setVisibility(emulation.getSelectedItem()
				.equals("RESID") ? View.VISIBLE : View.GONE);
		stereoFiltersRESID6581.setVisibility(emulation.getSelectedItem()
				.equals("RESID") ? View.VISIBLE : View.GONE);
		stereoFiltersRESID8580txt.setVisibility(emulation.getSelectedItem()
				.equals("RESID") ? View.VISIBLE : View.GONE);
		stereoFiltersRESID8580.setVisibility(emulation.getSelectedItem()
				.equals("RESID") ? View.VISIBLE : View.GONE);
		TextView stereoFiltersRESIDfp6581txt = (TextView) findViewById(R.id.stereoFiltersRESIDfp6581txt);
		Spinner stereoFiltersRESIDfp6581 = (Spinner) findViewById(R.id.stereoFiltersRESIDfp6581);
		TextView stereoFiltersRESIDfp8580txt = (TextView) findViewById(R.id.stereoFiltersRESIDfp8580txt);
		Spinner stereoFiltersRESIDfp8580 = (Spinner) findViewById(R.id.stereoFiltersRESIDfp8580);
		stereoFiltersRESIDfp6581txt.setVisibility(emulation.getSelectedItem()
				.equals("RESIDFP") ? View.VISIBLE : View.GONE);
		stereoFiltersRESIDfp6581.setVisibility(emulation.getSelectedItem()
				.equals("RESIDFP") ? View.VISIBLE : View.GONE);
		stereoFiltersRESIDfp8580txt.setVisibility(emulation.getSelectedItem()
				.equals("RESIDFP") ? View.VISIBLE : View.GONE);
		stereoFiltersRESIDfp8580.setVisibility(emulation.getSelectedItem()
				.equals("RESIDFP") ? View.VISIBLE : View.GONE);
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

				new FiltersRequest(appName, connection, REST_FILTERS_URL) {
					@Override
					protected void onPostExecute(List<String> filters) {
						if (filters == null) {
							return;
						}
						ArrayList<String> filtersRESID6581 = new ArrayList<String>();
						ArrayList<String> filtersRESID8580 = new ArrayList<String>();
						ArrayList<String> filtersRESIDfp6581 = new ArrayList<String>();
						ArrayList<String> filtersRESIDfp8580 = new ArrayList<String>();
						int idxFILTERRESID6581 = 0, idxFILTERRESID8580 = 0, idxFILTERRESIDfp6581 = 0, idxFILTERRESIDfp8580 = 0;
						int idxSTEREOFILTERRESID6581 = 0, idxSTEREOFILTERRESID8580 = 0, idxSTEREOFILTERRESIDfp6581 = 0, idxSTEREOFILTERRESIDfp8580 = 0;
						for (Iterator<String> iterator = filters.iterator(); iterator
								.hasNext();) {
							String filter = (String) iterator.next();
							if (filter.startsWith("RESID_MOS6581_")) {
								String substring = filter
										.substring("RESID_MOS6581_".length());
								if (substring.equals(preferences.getString(
										PAR_FILTERRESID6581,
										DEFAULT_FILTERRESID6581))) {
									idxFILTERRESID6581 = filtersRESID6581
											.size();
								}
								if (substring.equals(preferences.getString(
										PAR_STEREO_FILTERRESID6581,
										DEFAULT_FILTERRESID6581))) {
									idxSTEREOFILTERRESID6581 = filtersRESID6581
											.size();
								}
								filtersRESID6581.add(substring);
							} else if (filter.startsWith("RESID_MOS8580_")) {
								String substring = filter
										.substring("RESID_MOS8580_".length());
								if (substring.equals(preferences.getString(
										PAR_FILTERRESID8580,
										DEFAULT_FILTERRESID8580))) {
									idxFILTERRESID8580 = filtersRESID8580
											.size();
								}
								if (substring.equals(preferences.getString(
										PAR_STEREO_FILTERRESID8580,
										DEFAULT_FILTERRESID8580))) {
									idxSTEREOFILTERRESID8580 = filtersRESID8580
											.size();
								}
								filtersRESID8580.add(substring);
							} else if (filter.startsWith("RESIDFP_MOS6581_")) {
								String substring = filter
										.substring("RESIDFP_MOS6581_".length());
								if (substring.equals(preferences.getString(
										PAR_FILTERRESIDfp6581,
										DEFAULT_FILTERRESIDFP6581))) {
									idxFILTERRESIDfp6581 = filtersRESIDfp6581
											.size();
								}
								if (substring.equals(preferences.getString(
										PAR_STEREO_FILTERRESIDfp6581,
										DEFAULT_FILTERRESIDFP6581))) {
									idxSTEREOFILTERRESIDfp6581 = filtersRESIDfp6581
											.size();
								}
								filtersRESIDfp6581.add(substring);
							} else if (filter.startsWith("RESIDFP_MOS8580_")) {
								String substring = filter
										.substring("RESIDFP_MOS8580_".length());
								if (substring.equals(preferences.getString(
										PAR_FILTERRESIDfp8580,
										DEFAULT_FILTERRESIDFP8580))) {
									idxFILTERRESIDfp8580 = filtersRESIDfp8580
											.size();
								}
								if (substring.equals(preferences.getString(
										PAR_STEREO_FILTERRESIDfp8580,
										DEFAULT_FILTERRESIDFP8580))) {
									idxSTEREOFILTERRESIDfp8580 = filtersRESIDfp8580
											.size();
								}
								filtersRESIDfp8580.add(substring);
							}
						}
						setupFilterSpinners(
								filtersRESID6581, filtersRESID8580,
								filtersRESIDfp6581, filtersRESIDfp8580,
								idxFILTERRESID6581, idxFILTERRESID8580,
								idxFILTERRESIDfp6581, idxFILTERRESIDfp8580,
								idxSTEREOFILTERRESID6581, idxSTEREOFILTERRESID8580,
								idxSTEREOFILTERRESIDfp6581, idxSTEREOFILTERRESIDfp8580
								);
					}
				}.execute();

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
			Spinner emulation = (Spinner) findViewById(R.id.emulation);
			CheckBox songLength = (CheckBox) findViewById(R.id.songlength);
			EditText defaultLength = (EditText) findViewById(R.id.defaultLength);
			Spinner defaultModel = (Spinner) findViewById(R.id.defaultModel);
			CheckBox single = (CheckBox) findViewById(R.id.singleSong);
			CheckBox loop = (CheckBox) findViewById(R.id.loop);
			Spinner filters6581 = (Spinner) findViewById(R.id.filtersRESID6581);
			Spinner filters8580 = (Spinner) findViewById(R.id.filtersRESID8580);
			Spinner filtersRESIDfp6581 = (Spinner) findViewById(R.id.filtersRESIDfp6581);
			Spinner filtersRESIDfp8580 = (Spinner) findViewById(R.id.filtersRESIDfp8580);

			Spinner stereoFilters6581 = (Spinner) findViewById(R.id.stereoFiltersRESID6581);
			Spinner stereoFilters8580 = (Spinner) findViewById(R.id.stereoFiltersRESID8580);
			Spinner stereoFiltersRESIDfp6581 = (Spinner) findViewById(R.id.stereoFiltersRESIDfp6581);
			Spinner stereoFiltersRESIDfp8580 = (Spinner) findViewById(R.id.stereoFiltersRESIDfp8580);

			String query = "";
			query += PAR_EMULATION + "=" + emulation.getSelectedItem() + "&";
			query += PAR_ENABLE_DATABASE + "=" + songLength.isChecked() + "&";
			query += PAR_DEFAULT_PLAY_LENGTH + "=" + getNumber(defaultLength)
					+ "&";
			query += PAR_DEFAULT_MODEL + "=" + defaultModel.getSelectedItem()
					+ "&";
			query += PAR_SINGLE_SONG + "=" + single.isChecked() + "&";
			query += PAR_LOOP + "=" + loop.isChecked() + "&";

			query += PAR_FILTERRESID6581 + "=" + filters6581.getSelectedItem()
					+ "&";
			query += PAR_FILTERRESID8580 + "=" + filters8580.getSelectedItem()
					+ "&";
			query += PAR_FILTERRESIDfp6581 + "="
					+ filtersRESIDfp6581.getSelectedItem() + "&";
			query += PAR_FILTERRESIDfp8580 + "="
					+ filtersRESIDfp8580.getSelectedItem() + "&";

			query += PAR_STEREO_FILTERRESID6581 + "=" + stereoFilters6581.getSelectedItem()
					+ "&";
			query += PAR_STEREO_FILTERRESID8580 + "=" + stereoFilters8580.getSelectedItem()
					+ "&";
			query += PAR_STEREO_FILTERRESIDfp6581 + "="
					+ stereoFiltersRESIDfp6581.getSelectedItem() + "&";
			query += PAR_STEREO_FILTERRESIDfp8580 + "="
					+ stereoFiltersRESIDfp8580.getSelectedItem();
			
			URI uri = new URI("http", connection.getUsername() + ":"
					+ connection.getPassword(), connection.getHostname(),
					Integer.valueOf(connection.getPort()), REST_CONVERT_URL
							+ tune, query, null);

			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(uri.toString()), "audio/mpeg");
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

	private void setupFilterSpinners(ArrayList<String> filtersRESID6581,
			ArrayList<String> filtersRESID8580,
			ArrayList<String> filtersRESIDfp6581,
			ArrayList<String> filtersRESIDfp8580,
			int idxFILTERRESID6581,
			int idxFILTERRESID8580,
			int idxFILTERRESIDfp6581,
			int idxFILTERRESIDfp8580,
			int idxSTEREOFILTERRESID6581,
			int idxSTEREOFILTERRESID8580,
			int idxSTEREOFILTERRESIDfp6581,
			int idxSTEREOFILTERRESIDfp8580
			) {
		Spinner filters6581 = (Spinner) findViewById(R.id.filtersRESID6581);
		filters6581.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESID6581));
		filters6581.setSelection(idxFILTERRESID6581);
		filters6581.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_FILTERRESID6581,
						((Spinner) findViewById(R.id.filtersRESID6581))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		Spinner filters8580 = (Spinner) findViewById(R.id.filtersRESID8580);
		filters8580.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESID8580));
		filters8580.setSelection(idxFILTERRESID8580);
		filters8580.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_FILTERRESID8580,
						((Spinner) findViewById(R.id.filtersRESID8580))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		Spinner filtersfp6581 = (Spinner) findViewById(R.id.filtersRESIDfp6581);
		filtersfp6581.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESIDfp6581));
		filtersfp6581.setSelection(idxFILTERRESIDfp6581);
		filtersfp6581.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_FILTERRESIDfp6581,
						((Spinner) findViewById(R.id.filtersRESIDfp6581))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		Spinner filtersfp8580 = (Spinner) findViewById(R.id.filtersRESIDfp8580);
		filtersfp8580.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESIDfp8580));
		filtersfp8580.setSelection(idxFILTERRESIDfp8580);
		filtersfp8580.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_FILTERRESIDfp8580,
						((Spinner) findViewById(R.id.filtersRESIDfp8580))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

	
		Spinner stereoFilters6581 = (Spinner) findViewById(R.id.stereoFiltersRESID6581);
		stereoFilters6581.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESID6581));
		stereoFilters6581.setSelection(idxSTEREOFILTERRESID6581);
		stereoFilters6581.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_STEREO_FILTERRESID6581,
						((Spinner) findViewById(R.id.stereoFiltersRESID6581))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		Spinner stereoFilters8580 = (Spinner) findViewById(R.id.stereoFiltersRESID8580);
		stereoFilters8580.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESID8580));
		stereoFilters8580.setSelection(idxSTEREOFILTERRESID8580);
		stereoFilters8580.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_STEREO_FILTERRESID8580,
						((Spinner) findViewById(R.id.stereoFiltersRESID8580))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		Spinner stereoFiltersfp6581 = (Spinner) findViewById(R.id.stereoFiltersRESIDfp6581);
		stereoFiltersfp6581.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESIDfp6581));
		stereoFiltersfp6581.setSelection(idxSTEREOFILTERRESIDfp6581);
		stereoFiltersfp6581.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_STEREO_FILTERRESIDfp6581,
						((Spinner) findViewById(R.id.stereoFiltersRESIDfp6581))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		Spinner stereoFiltersfp8580 = (Spinner) findViewById(R.id.stereoFiltersRESIDfp8580);
		stereoFiltersfp8580.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item, filtersRESIDfp8580));
		stereoFiltersfp8580.setSelection(idxSTEREOFILTERRESIDfp8580);
		stereoFiltersfp8580.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PAR_STEREO_FILTERRESIDfp8580,
						((Spinner) findViewById(R.id.stereoFiltersRESIDfp8580))
								.getSelectedItem().toString());
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

}
