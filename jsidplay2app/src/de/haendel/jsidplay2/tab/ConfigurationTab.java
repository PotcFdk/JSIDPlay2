package de.haendel.jsidplay2.tab;

import static de.haendel.jsidplay2.config.IConfiguration.DECIMATE;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_BUFFER_SIZE;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_CBR;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_DIGI_BOOSTED_8580;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_ENABLE_DATABASE;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_LOOP;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_PLAY_LENGTH;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_RESIDFP_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_RESIDFP_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_SINGLE_SONG;
import static de.haendel.jsidplay2.config.IConfiguration.DEFAULT_VBR;
import static de.haendel.jsidplay2.config.IConfiguration.MOS6581;
import static de.haendel.jsidplay2.config.IConfiguration.MOS8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_BUFFER_SIZE;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_CBR;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_DEFAULT_MODEL;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_DEFAULT_PLAY_LENGTH;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_DIGI_BOOSTED_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_EMULATION;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_ENABLE_DATABASE;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_FREQUENCY;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_LOOP;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_STEREO_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_RESIDFP_STEREO_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_SAMPLING_METHOD;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_SINGLE_SONG;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_STEREO_FILTER_6581;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_STEREO_FILTER_8580;
import static de.haendel.jsidplay2.config.IConfiguration.PAR_VBR;
import static de.haendel.jsidplay2.config.IConfiguration.RESAMPLE;
import static de.haendel.jsidplay2.config.IConfiguration.RESID;
import static de.haendel.jsidplay2.config.IConfiguration.RESIDFP;
import static de.haendel.jsidplay2.config.IConfiguration._44100;
import static de.haendel.jsidplay2.config.IConfiguration._48000;
import static de.haendel.jsidplay2.config.IConfiguration._96000;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.TabBase;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.FiltersRequest;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;

public class ConfigurationTab extends TabBase {
	private static final String PREFIX_FILTER_6581 = "RESID_MOS6581_";
	private static final String PREFIX_FILTER_8580 = "RESID_MOS8580_";
	private static final String PREFIX_RESIDFP_FILTER_6581 = "RESIDFP_MOS6581_";
	private static final String PREFIX_RESIDFP_FILTER_8580 = "RESIDFP_MOS8580_";

	public class ConfigurationUIHelper extends UIHelper {

		public ConfigurationUIHelper(SharedPreferences preferences) {
			super(preferences);
		}

		@Override
		protected void spinnerUpdated(final String parName,
				final String newValue) {
			if (PAR_EMULATION.equals(parName)) {
				boolean isReSid = newValue.equals("RESID");
				updateFiltersVisibility(
						new View[] { filter6581txt, filter6581, filter8580txt,
								filter8580, stereoFilter6581txt,
								stereoFilter6581, stereoFilter8580txt,
								stereoFilter8580 }, isReSid);

				boolean isReSidFp = newValue.equals("RESIDFP");
				updateFiltersVisibility(new View[] { reSIDfpFilter6581txt,
						reSIDfpFilter6581, reSIDfpFilter8580txt,
						reSIDfpFilter8580, reSIDfpStereoFilter6581txt,
						reSIDfpStereoFilter6581, reSIDfpStereoFilter8580txt,
						reSIDfpStereoFilter8580 }, isReSidFp);
				configuration.setDefaultEmulation(newValue);
			} else if (PAR_DEFAULT_MODEL.equals(parName)) {
				configuration.setDefaultModel(newValue);
			} else if (PAR_FREQUENCY.equals(parName)) {
				configuration.setFrequency(newValue);
			} else if (PAR_SAMPLING_METHOD.equals(parName)) {
				configuration.setSamplingMethod(newValue);
			} else if (PAR_FILTER_6581.equals(parName)) {
				configuration.setFilter6581(newValue);
			} else if (PAR_FILTER_8580.equals(parName)) {
				configuration.setFilter8580(newValue);
			} else if (PAR_RESIDFP_FILTER_6581.equals(parName)) {
				configuration.setReSIDfpFilter6581(newValue);
			} else if (PAR_RESIDFP_FILTER_8580.equals(parName)) {
				configuration.setReSIDfpFilter8580(newValue);
			} else if (PAR_STEREO_FILTER_6581.equals(parName)) {
				configuration.setStereoFilter6581(newValue);
			} else if (PAR_STEREO_FILTER_8580.equals(parName)) {
				configuration.setStereoFilter8580(newValue);
			} else if (PAR_RESIDFP_STEREO_FILTER_6581.equals(parName)) {
				configuration.setReSIDfpStereoFilter6581(newValue);
			} else if (PAR_RESIDFP_STEREO_FILTER_8580.equals(parName)) {
				configuration.setReSIDfpStereoFilter8580(newValue);
			}
		}

		@Override
		protected void editTextUpdated(String parName, String newValue) {
			if (PAR_BUFFER_SIZE.equals(parName)) {
				configuration.setBufferSize(newValue);
			} else if (PAR_DEFAULT_PLAY_LENGTH.equals(parName)) {
				configuration.setDefaultLength(newValue);
			} else if (PAR_CBR.equals(parName)) {
				configuration.setCbr(newValue);
			} else if (PAR_VBR.equals(parName)) {
				configuration.setVbr(newValue);
			}
		}

		@Override
		protected void checkBoxUpdated(String parName, boolean newValue) {
			if (PAR_DIGI_BOOSTED_8580.equals(parName)) {
				configuration.setDigiBoosted8580(newValue);
			} else if (PAR_ENABLE_DATABASE.equals(parName)) {
				configuration.setEnableDatabase(newValue);
			} else if (PAR_LOOP.equals(parName)) {
				configuration.setLoop(newValue);
			} else if (PAR_SINGLE_SONG.equals(parName)) {
				configuration.setSingleSong(newValue);
			}
		}

		private void updateFiltersVisibility(View[] views, boolean visible) {
			for (int i = 0; i < views.length; i++) {
				View view = views[i];
				view.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		}

	}

	private EditText bufferSize, defaultLength;
	private CheckBox enableDatabase, singleSong, loop, digiBoosted8580;
	private Spinner emulation, defaultModel;

	private Spinner filter6581, filter8580, reSIDfpFilter6581,
			reSIDfpFilter8580;
	private TextView filter6581txt, filter8580txt, reSIDfpFilter6581txt,
			reSIDfpFilter8580txt;

	private Spinner samplingMethod, frequency, stereoFilter6581,
			stereoFilter8580, reSIDfpStereoFilter6581, reSIDfpStereoFilter8580;
	private TextView stereoFilter6581txt, stereoFilter8580txt,
			reSIDfpStereoFilter6581txt, reSIDfpStereoFilter8580txt;
	private EditText cbr, vbr;

	private SharedPreferences preferences;
	private UIHelper ui;

	public ConfigurationTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		super(activity, appName, configuration, tabHost);
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new ConfigurationUIHelper(preferences);

		tabHost.addTab(tabHost
				.newTabSpec(ConfigurationTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_cfg))
				.setContent(R.id.settings));

		bufferSize = (EditText) activity.findViewById(R.id.bufferSize);
		defaultLength = (EditText) activity.findViewById(R.id.defaultLength);
		enableDatabase = (CheckBox) activity.findViewById(R.id.enableDatabase);
		singleSong = (CheckBox) activity.findViewById(R.id.singleSong);
		loop = (CheckBox) activity.findViewById(R.id.loop);
		digiBoosted8580 = (CheckBox) activity
				.findViewById(R.id.digiBoosted8580);
		emulation = (Spinner) activity.findViewById(R.id.emulation);
		defaultModel = (Spinner) activity.findViewById(R.id.defaultModel);
		samplingMethod = (Spinner) activity.findViewById(R.id.samplingMethod);
		frequency = (Spinner) activity.findViewById(R.id.frequency);

		filter6581 = (Spinner) activity.findViewById(R.id.filter6581);
		filter6581txt = (TextView) activity.findViewById(R.id.filter6581txt);
		filter8580 = (Spinner) activity.findViewById(R.id.filter8580);
		filter8580txt = (TextView) activity.findViewById(R.id.filter8580txt);
		reSIDfpFilter6581 = (Spinner) activity
				.findViewById(R.id.reSIDfpFilter6581);
		reSIDfpFilter6581txt = (TextView) activity
				.findViewById(R.id.reSIDfpFilter6581txt);
		reSIDfpFilter8580 = (Spinner) activity
				.findViewById(R.id.reSIDfpFilter8580);
		reSIDfpFilter8580txt = (TextView) activity
				.findViewById(R.id.reSIDfpFilter8580txt);

		stereoFilter6581 = (Spinner) activity
				.findViewById(R.id.stereoFilter6581);
		stereoFilter6581txt = (TextView) activity
				.findViewById(R.id.stereoFilter6581txt);
		stereoFilter8580 = (Spinner) activity
				.findViewById(R.id.stereoFilter8580);
		stereoFilter8580txt = (TextView) activity
				.findViewById(R.id.stereoFilter8580txt);
		reSIDfpStereoFilter6581 = (Spinner) activity
				.findViewById(R.id.reSIDfpStereoFilter6581);
		reSIDfpStereoFilter6581txt = (TextView) activity
				.findViewById(R.id.reSIDfpStereoFilter6581txt);
		reSIDfpStereoFilter8580 = (Spinner) activity
				.findViewById(R.id.reSIDfpStereoFilter8580);
		reSIDfpStereoFilter8580txt = (TextView) activity
				.findViewById(R.id.reSIDfpStereoFilter8580txt);
		cbr = (EditText) activity.findViewById(R.id.cbr);
		vbr = (EditText) activity.findViewById(R.id.vbr);

		ui.setupEditText(bufferSize, PAR_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);

		ui.setupEditText(defaultLength, PAR_DEFAULT_PLAY_LENGTH,
				DEFAULT_PLAY_LENGTH);

		ui.setupCheckBox(enableDatabase, PAR_ENABLE_DATABASE,
				DEFAULT_ENABLE_DATABASE);
		ui.setupCheckBox(singleSong, PAR_SINGLE_SONG, DEFAULT_SINGLE_SONG);
		ui.setupCheckBox(loop, PAR_LOOP, DEFAULT_LOOP);
		ui.setupCheckBox(digiBoosted8580, PAR_DIGI_BOOSTED_8580,
				DEFAULT_DIGI_BOOSTED_8580);

		ui.setupSpinner(activity, emulation, new String[] { RESID, RESIDFP },
				PAR_EMULATION, RESIDFP);
		ui.setupSpinner(activity, defaultModel,
				new String[] { MOS6581, MOS8580 }, PAR_DEFAULT_MODEL, MOS6581);
		ui.setupSpinner(activity, samplingMethod, new String[] { DECIMATE,
				RESAMPLE }, PAR_SAMPLING_METHOD, DECIMATE);
		ui.setupSpinner(activity, frequency, new String[] { _44100, _48000,
				_96000 }, PAR_FREQUENCY, _48000);

		ui.setupEditText(cbr, PAR_CBR, DEFAULT_CBR);
		ui.setupEditText(vbr, PAR_VBR, DEFAULT_VBR);
		requestFilters();
	}

	private void requestFilters() {
		new FiltersRequest(appName, configuration, RequestType.FILTERS, "") {
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

				ui.setupSpinner(activity, filter6581, filter6581List
						.toArray(new String[0]), PAR_FILTER_6581, preferences
						.getString(PAR_FILTER_6581, DEFAULT_FILTER_6581));
				ui.setupSpinner(activity, filter8580, filter8580List
						.toArray(new String[0]), PAR_FILTER_8580, preferences
						.getString(PAR_FILTER_8580, DEFAULT_FILTER_8580));
				ui.setupSpinner(activity, reSIDfpFilter6581,
						reSidFpFilter6581List.toArray(new String[0]),
						PAR_RESIDFP_FILTER_6581, preferences.getString(
								PAR_RESIDFP_FILTER_6581,
								DEFAULT_RESIDFP_FILTER_6581));
				ui.setupSpinner(activity, reSIDfpFilter8580,
						reSidFpFilter8580List.toArray(new String[0]),
						PAR_RESIDFP_FILTER_8580, preferences.getString(
								PAR_RESIDFP_FILTER_8580,
								DEFAULT_RESIDFP_FILTER_8580));

				ui.setupSpinner(activity, stereoFilter6581, filter6581List
						.toArray(new String[0]), PAR_STEREO_FILTER_6581,
						preferences.getString(PAR_STEREO_FILTER_6581,
								DEFAULT_FILTER_6581));
				ui.setupSpinner(activity, stereoFilter8580, filter8580List
						.toArray(new String[0]), PAR_STEREO_FILTER_8580,
						preferences.getString(PAR_STEREO_FILTER_8580,
								DEFAULT_FILTER_8580));
				ui.setupSpinner(activity, reSIDfpStereoFilter6581,
						reSidFpFilter6581List.toArray(new String[0]),
						PAR_RESIDFP_STEREO_FILTER_6581, preferences.getString(
								PAR_RESIDFP_STEREO_FILTER_6581,
								DEFAULT_RESIDFP_FILTER_6581));
				ui.setupSpinner(activity, reSIDfpStereoFilter8580,
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

}
