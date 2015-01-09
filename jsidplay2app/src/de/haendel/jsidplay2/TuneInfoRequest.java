package de.haendel.jsidplay2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpEntity;

import android.util.Pair;

public abstract class TuneInfoRequest extends
		LongRunningRequest<List<Pair<String, String>>> {
	public TuneInfoRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
	}

	@Override
	protected List<Pair<String, String>> getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		InputStream content = httpEntity.getContent();
		final StringBuffer out = new StringBuffer();
		int n = 1;
		while (n > 0) {
			byte[] b = new byte[4096];
			n = content.read(b);
			if (n > 0)
				out.append(new String(b, 0, n));
		}
		// out is string containing a map
		List<Pair<String, String>> rows = new ArrayList<Pair<String, String>>();

		String mapToken = out.substring(1, out.length() - 1);
		String[] splittedMap = LongRunningRequest.splitJSONToken(mapToken, ",");
		for (String mapEntryToken : splittedMap) {
			String[] splittedMapEntry = LongRunningRequest.splitJSONToken(
					mapEntryToken, ":");
			String tuneInfoName = null;
			String tuneInfoValue = "";
			for (String keyOrValueToken : splittedMapEntry) {
				String keyOrValue = keyOrValueToken.substring(1,
						keyOrValueToken.length() - 1);
				// newline handling
				keyOrValue = keyOrValue.replaceAll("\\\\n", "\n");
				if (tuneInfoName == null) {
					// localize name
					tuneInfoName = getString(keyOrValue);
				} else {
					tuneInfoValue = keyOrValue;
				}
			}
			// sort out empty tune infos
			if (!tuneInfoValue.equals("")) {
				Pair<String, String> p = new Pair<String, String>(tuneInfoName,
						tuneInfoValue);
				rows.add(p);
			}
		}
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
	protected abstract String getString(String key);
}
