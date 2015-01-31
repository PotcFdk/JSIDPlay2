package de.haendel.jsidplay2.common;

import android.app.Activity;
import android.widget.TabHost;
import de.haendel.jsidplay2.config.IConfiguration;

public class TabBase {

	protected Activity activity;
	protected String appName;
	protected IConfiguration configuration;
	protected TabHost tabHost;

	protected TabBase(final Activity activity, final String appName,
			final IConfiguration configuration, final TabHost tabHost) {
		this.activity = activity;
		this.appName = appName;
		this.configuration = configuration;
		this.tabHost = tabHost;
	}
}
