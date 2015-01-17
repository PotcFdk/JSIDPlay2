package de.haendel.jsidplay2;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;

public class FiltersRequest extends JSIDPlay2RESTRequest<List<String>> {

	public FiltersRequest(String appName, IConfiguration configuration, RequestType type, String url) {
		super(appName, configuration, type, url);
	}

	@Override
	protected List<String> getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		return receiveList(httpEntity);
	}
}
