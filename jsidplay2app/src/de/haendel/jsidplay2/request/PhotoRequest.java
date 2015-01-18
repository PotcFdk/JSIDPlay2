package de.haendel.jsidplay2.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;

import de.haendel.jsidplay2.config.IConfiguration;

public class PhotoRequest extends JSIDPlay2RESTRequest<byte[]> {
	public PhotoRequest(String appName, IConfiguration configuration, RequestType type, String url) {
		super(appName, configuration, type, url, null);
	}

	@Override
	protected byte[] getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		InputStream content = httpEntity.getContent();
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		int n = 1;
		while (n > 0) {
			byte[] b = new byte[4096];
			n = content.read(b);
			if (n > 0)
				s.write(b, 0, n);
		}
		return s.toByteArray();
	}
	
}
