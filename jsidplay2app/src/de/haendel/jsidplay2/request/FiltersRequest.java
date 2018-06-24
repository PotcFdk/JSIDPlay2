package de.haendel.jsidplay2.request;

import java.io.IOException;
import java.net.URLConnection;
import java.util.List;

import de.haendel.jsidplay2.config.IConfiguration;

public class FiltersRequest extends JSIDPlay2RESTRequest<List<String>> {

	public FiltersRequest(String appName, IConfiguration configuration, RequestType type, String url) {
		super(appName, configuration, type, url, null);
	}

	@Override
	protected List<String> getResult(URLConnection connection) throws IllegalStateException, IOException {
		return receiveList(connection);
	}
}
