package de.haendel.jsidplay2;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpEntity;

import android.util.Pair;

public abstract class TuneInfoRequest extends
		LongRunningRequest<List<Pair<String, String>>> implements LongRunningRequest.IKeyLocalizer {
	public TuneInfoRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
	}

	@Override
	protected List<Pair<String, String>> getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		List<Pair<String, String>> rows = receiveListOfKeyValues(httpEntity, this);

		Comparator<? super Pair<String, String>> rowC = new Comparator<Pair<String, String>>() {
			@Override
			public int compare(Pair<String, String> lhs,
					Pair<String, String> rhs) {
				return lhs.first.compareTo(rhs.first);
			}
		};
		Collections.sort(rows, rowC);
		return rows;
	}

	/**
	 * Get localized tune info name
	 * 
	 * @param key
	 *            tune info name
	 * @return localized string
	 */
	public abstract String getString(String key);
}
