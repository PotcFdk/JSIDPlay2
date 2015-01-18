package de.haendel.jsidplay2.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import de.haendel.jsidplay2.config.IConfiguration;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

public abstract class JSIDPlay2RESTRequest<ResultType> extends
		AsyncTask<String, Void, ResultType> {

	public enum RequestType {
		DOWNLOAD(REST_DOWNLOAD_URL), CONVERT(REST_CONVERT_URL), DIRECTORY(
				REST_DIRECTORY_URL), PHOTO(REST_PHOTO_URL), INFO(REST_INFO), FILTERS(
				REST_FILTERS_URL);

		private String url;

		private RequestType(String url) {
			this.url = url;
		}

		public String getUrl() {
			return url;
		}
	}

	private static final String CONTEXT_ROOT = "/jsidplay2service";
	private static final String ROOT_PATH = "/JSIDPlay2REST";
	private static final String ROOT_URL = CONTEXT_ROOT + ROOT_PATH;

	private static final String REST_DOWNLOAD_URL = ROOT_URL + "/download";
	private static final String REST_CONVERT_URL = ROOT_URL + "/convert";
	private static final String REST_DIRECTORY_URL = ROOT_URL + "/directory";
	private static final String REST_PHOTO_URL = ROOT_URL + "/photo";
	private static final String REST_INFO = ROOT_URL + "/info";
	private static final String REST_FILTERS_URL = ROOT_URL + "/filters";

	private static final int CONNECTION_TIMEOUT = 10000;
	private static final int SOCKET_TIMEOUT = 10000;

	protected final String appName;
	protected IConfiguration configuration;
	protected String url, query;

	public JSIDPlay2RESTRequest(String appName, IConfiguration configuration,
			RequestType type, String url, String query) {
		this.appName = appName;
		this.configuration = configuration;
		this.url = type.getUrl() + url;
		this.query = query;
	}

	@Override
	protected ResultType doInBackground(String... params) {
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();

			HttpParams httpParams = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams,
					CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);

			httpClient.getCredentialsProvider().setCredentials(
					new AuthScope(configuration.getHostname(), AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(configuration.getUsername(), configuration
							.getPassword()));

			URI myUri = new URI("http", null, configuration.getHostname(),
					Integer.valueOf(configuration.getPort()), url, query, null);

			Log.d(appName, "HTTP-GET: " + myUri);

			HttpGet httpGet = new HttpGet(myUri);
			HttpContext localContext = new BasicHttpContext();
			HttpResponse response = httpClient.execute(httpGet, localContext);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpURLConnection.HTTP_OK) {
				return getResult(response.getEntity());
			} else {
				Log.e(appName,
						String.format("URL: '%s', HTTP status: '%d':",
								myUri.toString(), statusCode));
				return null;
			}
		} catch (IOException e) {
			Log.e(appName, e.getMessage(), e);
			return null;
		} catch (URISyntaxException e) {
			Log.e(appName, e.getMessage(), e);
			return null;
		}
	}

	protected abstract ResultType getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException;

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

	protected ArrayList<String> receiveList(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		InputStream content = httpEntity.getContent();
		StringBuffer out = new StringBuffer();
		int n = 1;
		while (n > 0) {
			byte[] b = new byte[4096];
			n = content.read(b);
			if (n > 0)
				out.append(new String(b, 0, n));
		}
		String line = out.substring(1, out.length() - 1);
		String[] childs = splitJSONToken(line, ",");
		ArrayList<String> list = new ArrayList<String>();
		for (String child : childs) {
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

	protected List<Pair<String, String>> receiveListOfKeyValues(
			HttpEntity httpEntity, IKeyLocalizer localizer)
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
		String[] splittedMap = JSIDPlay2RESTRequest.splitJSONToken(mapToken,
				",");
		for (String mapEntryToken : splittedMap) {
			String[] splittedMapEntry = JSIDPlay2RESTRequest.splitJSONToken(
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
					tuneInfoName = localizer.getString(keyOrValue);
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
		return rows;
	}
}