package de.haendel.jsidplay2.tab;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.TabHost;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;

public class GeneralTab {

	private static final String PAR_HOSTNAME = "hostname";
	private static final String PAR_PORT = "port";
	private static final String PAR_USERNAME = "username";
	private static final String PAR_PASSWORD = "password";

	private static final String DEFAULT_HOSTNAME = "haendel.ddns.net";
	private static final String DEFAULT_PORT = "8080";
	private static final String DEFAULT_USERNAME = "jsidplay2";
	private static final String DEFAULT_PASSWORD = "jsidplay2!";

	public class GeneralUIHelper extends UIHelper {
		public GeneralUIHelper(final SharedPreferences preferences) {
			super(preferences);
		}

		@Override
		protected void editTextUpdated(final String parName,
				final String newValue) {
			if (PAR_HOSTNAME.equals(parName)) {
				configuration.setHostname(newValue);
			} else if (PAR_PORT.equals(parName)) {
				configuration.setPort(newValue);
			} else if (PAR_USERNAME.equals(parName)) {
				configuration.setUsername(newValue);
			} else if (PAR_PASSWORD.equals(parName)) {
				configuration.setPassword(newValue);
			}
		}
	}

	private String appName;
	private IConfiguration configuration;
	private TabHost tabHost;
	private SharedPreferences preferences;

	private UIHelper ui;
	private EditText hostname, port, username, password;

	public GeneralTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		this.appName = appName;
		this.configuration = configuration;
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new GeneralUIHelper(preferences);
		tabHost.addTab(tabHost.newTabSpec(GeneralTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_connection))
				.setContent(R.id.general));

		hostname = (EditText) activity.findViewById(R.id.hostname);
		port = (EditText) activity.findViewById(R.id.port);
		username = (EditText) activity.findViewById(R.id.username);
		password = (EditText) activity.findViewById(R.id.password);

		ui.setupEditText(hostname, PAR_HOSTNAME, DEFAULT_HOSTNAME);
		ui.setupEditText(port, PAR_PORT, DEFAULT_PORT);
		ui.setupEditText(username, PAR_USERNAME, DEFAULT_USERNAME);
		ui.setupEditText(password, PAR_PASSWORD, DEFAULT_PASSWORD);

	}

}
