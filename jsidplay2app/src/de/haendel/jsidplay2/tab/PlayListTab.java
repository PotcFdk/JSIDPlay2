package de.haendel.jsidplay2.tab;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.haendel.jsidplay2.JSIDPlay2Service.PlayListEntry;
import de.haendel.jsidplay2.MainActivity;
import de.haendel.jsidplay2.R;
import de.haendel.jsidplay2.common.TabBase;
import de.haendel.jsidplay2.common.UIHelper;
import de.haendel.jsidplay2.config.IConfiguration;

public abstract class PlayListTab extends TabBase {

	private static final String PAR_RANDOM = "random";
	private static final String DEFAULT_RANDOM = Boolean.FALSE.toString();
	
	public class PlayListUIHelper extends UIHelper {

		public PlayListUIHelper(SharedPreferences preferences) {
			super(preferences);
		}

		@Override
		protected void checkBoxUpdated(String parName, boolean newValue) {
			if (parName.equals(PAR_RANDOM)) {
				PlayListTab.this.setRandomized(newValue);
			}
		}
	}

	private ScrollView favoritesScroll;
	private TableLayout favorites;
	private CheckBox random;
	
	private SharedPreferences preferences;
	private UIHelper ui;

	public PlayListTab(final MainActivity activity, final String appName,
			final IConfiguration configuration, TabHost tabHost) {
		super(activity, appName, configuration, tabHost);
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		ui = new PlayListUIHelper(preferences);

		tabHost.addTab(tabHost.newTabSpec(PlayListTab.class.getSimpleName())
				.setIndicator(activity.getString(R.string.tab_playlist))
				.setContent(R.id.playlist));

		favoritesScroll = (ScrollView) activity
				.findViewById(R.id.favoritesScroll);
		favorites = (TableLayout) activity.findViewById(R.id.favorites);
		random = (CheckBox) activity.findViewById(R.id.random);

		ui.setupCheckBox(random, PAR_RANDOM, DEFAULT_RANDOM);

	}

	public PlayListEntry addRow(final PlayListEntry entry) {
		final TableRow row = new TableRow(activity);
		row.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				play(entry);
			}
		});
		row.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.MATCH_PARENT));

		TextView col = new TextView(activity);
		col.setText(entry.getResource());
		col.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));
		row.addView(col);
		row.setBackgroundResource(R.drawable.selector);

		favorites.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT));

		favoritesScroll.post(new Runnable() {
			@Override
			public void run() {
				favoritesScroll.scrollTo(0, row.getTop());
			}
		});
		return entry;
	}

	public void removeLast() {
		favorites.removeViewAt(favorites.getChildCount() - 1);
	}

	public void gotoRow(final int currentSong) {
		favoritesScroll.post(new Runnable() {
			@Override
			public void run() {
				favoritesScroll.scrollTo(0, favorites.getChildAt(currentSong)
						.getTop());
			}
		});
	}

	protected abstract void play(PlayListEntry entry);
	
	protected abstract void setRandomized(boolean newValue);

}
