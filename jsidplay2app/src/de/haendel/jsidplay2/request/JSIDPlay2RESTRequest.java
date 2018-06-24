package de.haendel.jsidplay2.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import de.haendel.jsidplay2.config.IConfiguration;

public abstract class JSIDPlay2RESTRequest<ResultType> extends AsyncTask<String, Void, ResultType> {

	public enum RequestType {
		DOWNLOAD(REST_DOWNLOAD_URL), CONVERT(REST_CONVERT_URL), DIRECTORY(REST_DIRECTORY_URL), PHOTO(
				REST_PHOTO_URL), INFO(REST_INFO), FILTERS(REST_FILTERS_URL);

		private String url;

		private RequestType(String url) {
			this.url = url;
		}

		public String getUrl() {
			return url;
		}
	}

	private static final String UTF_8 = "UTF-8";
	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	private static final String REST_DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String REST_CONVERT_URL = ROOT_URL + "/convert";
	private static final String REST_DIRECTORY_URL = ROOT_URL + "/directory";
	private static final String REST_PHOTO_URL = ROOT_URL + "/photo";
	private static final String REST_INFO = ROOT_URL + "/info";
	private static final String REST_FILTERS_URL = ROOT_URL + "/filters";

	private static final int RETRY_PERIOD_S = 10;

	protected final String appName;
	protected IConfiguration configuration;
	protected String url;
	protected Map<String, String> properties;

	public JSIDPlay2RESTRequest(String appName, IConfiguration configuration, RequestType type, String url,
			Map<String, String> properties) {
		this.appName = appName;
		this.configuration = configuration;
		this.url = type.getUrl() + url;
		this.properties = properties;
	}

	@Override
	protected ResultType doInBackground(String... params) {
		while (true) {
			try {
				String query = "";
				if (properties != null) {
					for (Entry<String, String> property : properties.entrySet()) {
						query += "&" + property.getKey() + "=" + property.getValue();
					}
				}
				URI myUri = new URI("http", null, configuration.getHostname(), Integer.valueOf(configuration.getPort()),
						url, query, null);

				Log.d(appName, "HTTP-GET: " + myUri);

				Authenticator.setDefault(new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(configuration.getUsername(),
								configuration.getPassword().toCharArray());
					}
				});
				HttpURLConnection conn = (HttpURLConnection) myUri.toURL().openConnection();
				conn.setUseCaches(false);
				conn.setRequestMethod("GET");
				int statusCode = conn.getResponseCode();
				if (statusCode == HttpURLConnection.HTTP_OK) {
					return getResult(conn);
				} else {
					Log.e(appName, String.format("URL: '%s', HTTP status: '%d', Retry in '%d' seconds!",
							myUri.toString(), statusCode, RETRY_PERIOD_S));
				}
			} catch (IOException e) {
				Log.e(appName, e.getMessage(), e);
			} catch (URISyntaxException e) {
				Log.e(appName, e.getMessage(), e);
			}
			try {
				Thread.sleep(RETRY_PERIOD_S * 1000);
			} catch (InterruptedException e) {
				Log.e(appName, "Interrupted while sleeping!", e);
			}
		}
	}

	protected abstract ResultType getResult(URLConnection httpEntity) throws IllegalStateException, IOException;

	public static String[] splitJSONToken(String line, String sep) {
		String otherThanQuote = " [^\"] ";
		String quotedString = String.format(" \" %s* \" ", otherThanQuote);
		String regex = String.format("(?x) " + // enable comments,
												// ignore white spaces
				sep + "                         " + // match a comma
				"(?=                       " + // start positive look
												// ahead
				"  (                       " + // start group 1
				"    %s*                   " + // match 'otherThanQuote'
												// zero or more times
				"    %s                    " + // match 'quotedString'
				"  )*                      " + // end group 1 and repeat
												// it zero or more times
				"  %s*                     " + // match 'otherThanQuote'
				"  $                       " + // match the end of the
												// string
				")                         ", // stop positive look
												// ahead
				otherThanQuote, quotedString, otherThanQuote);
		return line.split(regex, -1);
	}

	protected ArrayList<String> receiveList(URLConnection connection) throws IllegalStateException, IOException {
		InputStream content = connection.getInputStream();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int n = 1;
		while (n > 0) {
			byte[] b = new byte[4096];
			n = content.read(b);
			if (n > 0)
				out.write(b, 0, n);
		}
		String trimmed = out.toString(UTF_8).trim();
		String line = trimmed.substring(1, trimmed.length() - 1);
		String[] childs = splitJSONToken(line, ",");
		ArrayList<String> list = new ArrayList<String>();
		for (String child : childs) {
			child = child.trim();
			if (child.length() > 2) {
				list.add(child.substring(1, child.length() - 1));
			}
		}
		return list;
	}

	public interface IKeyLocalizer {
		/**
		 * Get localized tune info name
		 * 
		 * @param key
		 *            tune info name
		 * @return localized string
		 */
		String getString(String key);
	}

	protected List<Pair<String, String>> receiveListOfKeyValues(URLConnection connection, IKeyLocalizer localizer)
			throws IllegalStateException, IOException {
		InputStream content = connection.getInputStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int n = 1;
		while (n > 0) {
			byte[] b = new byte[4096];
			n = content.read(b);
			if (n > 0)
				out.write(b, 0, n);
		}
		// out is string containing a map
		List<Pair<String, String>> rows = new ArrayList<Pair<String, String>>();

		String trimmed = out.toString(UTF_8).trim();
		String mapToken = trimmed.substring(1, trimmed.length() - 1);
		String[] splittedMap = JSIDPlay2RESTRequest.splitJSONToken(mapToken, ",");
		for (String mapEntryToken : splittedMap) {
			String[] splittedMapEntry = JSIDPlay2RESTRequest.splitJSONToken(mapEntryToken, ":");
			String tuneInfoName = null;
			String tuneInfoValue = "";
			for (String keyOrValueToken : splittedMapEntry) {
				keyOrValueToken = keyOrValueToken.trim();
				String keyOrValue = keyOrValueToken.substring(1, keyOrValueToken.length() - 1);
				// newline handling
				keyOrValue = keyOrValue.replaceAll("\\\\n", "\n");
				if (tuneInfoName == null) {
					// localize name
					tuneInfoName = localizer.getString(keyOrValue);
				} else {
					tuneInfoValue = keyOrValue;
				}
			}
			// sort out empty tune infos
			if (!tuneInfoValue.equals("")) {
				Pair<String, String> p = new Pair<String, String>(tuneInfoName, tuneInfoValue);
				rows.add(p);
			}
		}
		return rows;
	}
}