package de.haendel.jsidplay2.tab;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;

public class SidTab {

	private Context context;
	private String appName;
	private IConfiguration configuration;
	private TabHost tabHost;
	private SharedPreferences preferences;

	private UIHelper ui;
	private TextView resource;
	private ImageView image;
	private TableLayout table;

	public SidTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		this.context = activity;
		this.appName = appName;
		this.configuration = configuration;
		this.tabHost = tabHost;
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new UIHelper(preferences);
		TabSpec newTabSpec = tabHost.newTabSpec(SidTab.class.getSimpleName());
		tabHost.addTab(newTabSpec.setIndicator(
				activity.getString(R.string.tab_tune)).setContent(R.id.tune));

		resource = (TextView) activity.findViewById(R.id.resource);
		image = (ImageView) activity.findViewById(R.id.image);
		table = (TableLayout) activity.findViewById(R.id.table);
	}

	public void viewPhoto(byte[] photo) {
		Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
		image.setImageBitmap(bitmap);
	}

	void viewTuneInfos(List<Pair<String, String>> rows) {
		table.removeAllViews();
		for (Pair<String, String> r : rows) {
			TableRow tr = new TableRow(context);
			tr.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.MATCH_PARENT));

			TextView b = new TextView(context);
			b.setText(r.first);
			b.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			tr.addView(b);

			b = new TextView(context);
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

	public String getCurrentTune() {
		return resource.getText().toString();
	}

	public void setCurrentTune(String canonicalPath) {
		resource.setText(canonicalPath);
	}
}
