package de.haendel.jsidplay2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

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

import android.os.AsyncTask;
import android.util.Log;

public abstract class LongRunningRequest<ResultType> extends
		AsyncTask<String, Void, ResultType> {

	private static final int CONNECTION_TIMEOUT = 5000;
	private static final int SOCKET_TIMEOUT = 5000;

	protected final String appName;
	protected Connection conn;
	protected String url;

	public LongRunningRequest(String appName, Connection conn, String url) {
		this.appName = appName;
		this.conn = conn;
		this.url = url;
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
					new AuthScope(conn.getHostname(), AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(conn.getUsername(), conn
							.getPassword()));

			URI myUri = new URI("http", null, conn.getHostname(),
					Integer.valueOf(conn.getPort()), url,
					params.length != 0 ? "filter=" + params[0] : null, null);

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
}