package de.haendel.jsidplay2;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;

public class FiltersRequest extends LongRunningRequest<List<String>> {

	public FiltersRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
	}

	@Override
	protected List<String> getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		return receiveList(httpEntity);
	}
}
