package de.haendel.jsidplay2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;

public abstract class DirectoryRequest extends LongRunningRequest<List<String>> {

	public DirectoryRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
	}

	@Override
	protected List<String> getResult(HttpEntity httpEntity) throws IllegalStateException, IOException {
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
	
	@Override
	protected abstract void onPostExecute(List<String> result);
}
