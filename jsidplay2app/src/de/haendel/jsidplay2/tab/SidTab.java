package de.haendel.jsidplay2.tab;

import java.lang.reflect.Field;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.TabBase;
import de.haendel.jsidplay2.config.IConfiguration;
import de.haendel.jsidplay2.request.JSIDPlay2RESTRequest.RequestType;
import de.haendel.jsidplay2.request.PhotoRequest;
import de.haendel.jsidplay2.request.TuneInfoRequest;

public class SidTab extends TabBase {

	private TextView resource;
	private ImageView image;
	private TableLayout table;

	public SidTab(final Activity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		super(activity, appName, configuration, tabHost);

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
			TableRow tr = new TableRow(activity);
			tr.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.MATCH_PARENT));

			TextView b = new TextView(activity);
			b.setText(r.first);
			b.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			tr.addView(b);

			b = new TextView(activity);
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

	public void requestSidDetails(String canonicalPath) {
		setCurrentTune(canonicalPath);
		new PhotoRequest(appName, configuration, RequestType.PHOTO,
				canonicalPath) {
			@Override
			protected void onPostExecute(byte[] photo) {
				if (photo == null) {
					return;
				}
				viewPhoto(photo);
			}
		}.execute();
		new TuneInfoRequest(appName, configuration, RequestType.INFO,
				canonicalPath) {
			public String getString(String key) {
				key = key.replaceAll("[.]", "_");
				for (Field field : R.string.class.getDeclaredFields()) {
					if (field.getName().equals(key)) {
						try {
							return activity.getString(field.getInt(null));
						} catch (IllegalArgumentException e) {
						} catch (IllegalAccessException e) {
						}
					}
				}
				return "???";
			}

			@Override
			protected void onPostExecute(List<Pair<String, String>> out) {
				if (out == null) {
					return;
				}
				viewTuneInfos(out);
			}
		}.execute();
	}
}
