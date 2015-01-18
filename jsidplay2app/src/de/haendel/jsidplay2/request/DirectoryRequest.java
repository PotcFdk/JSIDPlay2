package de.haendel.jsidplay2.request;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;

import de.haendel.jsidplay2.config.IConfiguration;

public abstract class DirectoryRequest extends
		JSIDPlay2RESTRequest<List<String>> {

	private static final String FILTER_PAR = "filter";

	public DirectoryRequest(String appName, IConfiguration configuration,
			RequestType type, String url, String filter) {
		super(appName, configuration, type, url, FILTER_PAR + "=" + filter);
	}

	@Override
	protected List<String> getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		return receiveList(httpEntity);
	}

	@Override
	protected abstract void onPostExecute(List<String> result);
}
