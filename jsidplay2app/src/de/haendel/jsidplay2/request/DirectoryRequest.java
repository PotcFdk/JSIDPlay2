package de.haendel.jsidplay2.request;

import java.io.IOException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.haendel.jsidplay2.config.IConfiguration;

public abstract class DirectoryRequest extends JSIDPlay2RESTRequest<List<String>> {

	private static final String FILTER_PAR = "filter";
	private static final String TUNE_FILTER = ".*\\.(sid|dat|mus|str|mp3|mp4|jpg)$";

	private static final Map<String, String> parameters = new HashMap<String, String>();
	static {
		parameters.put(FILTER_PAR, TUNE_FILTER);
	}

	public DirectoryRequest(String appName, IConfiguration configuration, RequestType type, String url) {
		super(appName, configuration, type, url, parameters);
	}

	@Override
	protected List<String> getResult(URLConnection connection) throws IllegalStateException, IOException {
		return receiveList(connection);
	}

	@Override
	protected abstract void onPostExecute(List<String> result);
}
