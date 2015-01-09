package de.haendel.jsidplay2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;

public class PhotoRequest extends LongRunningRequest<byte[]> {
	public PhotoRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
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
