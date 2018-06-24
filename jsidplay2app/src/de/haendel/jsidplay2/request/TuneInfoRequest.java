package de.haendel.jsidplay2.request;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.util.Pair;
import de.haendel.jsidplay2.config.IConfiguration;

public abstract class TuneInfoRequest extends JSIDPlay2RESTRequest<List<Pair<String, String>>>
		implements JSIDPlay2RESTRequest.IKeyLocalizer {
	public TuneInfoRequest(String appName, IConfiguration configuration, RequestType type, String url) {
		super(appName, configuration, type, url, null);
	}

	@Override
	protected List<Pair<String, String>> getResult(URLConnection connection) throws IllegalStateException, IOException {
		List<Pair<String, String>> rows = receiveListOfKeyValues(connection, this);

		Comparator<? super Pair<String, String>> rowC = new Comparator<Pair<String, String>>() {
			@Override
			public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
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
