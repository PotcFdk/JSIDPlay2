package de.haendel.jsidplay2.request;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;

import de.haendel.jsidplay2.config.IConfiguration;
import android.net.Uri;
import android.os.Environment;

public class DownloadRequest extends JSIDPlay2RESTRequest<DataAndType> {
	private static final String DOWNLOAD_DIR = "Download";

	public DownloadRequest(String appName, IConfiguration configuration, RequestType type, String url) {
		super(appName, configuration, type, url, null);
	}

	@Override
	protected DataAndType getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		InputStream content = httpEntity.getContent();
		long length = httpEntity.getContentLength();
		File sdRootDir = Environment.getExternalStorageDirectory();
		File music = new File(new File(sdRootDir, DOWNLOAD_DIR),
				new File(url).getName());
		OutputStream out;
		byte[] b = new byte[4096];
		if (sdRootDir.canWrite()) {
			out = new BufferedOutputStream(new FileOutputStream(music));
			int count = 0;
			while (count < length) {
				int n = content.read(b);
				if (n > 0) {
					out.write(b, 0, n);
					count += n;
				}
			}
			out.close();
		}
		DataAndType dt = new DataAndType();
		dt.setUri(Uri.fromFile(music));
		dt.setType(url.endsWith(".mp3") ? "audio/mpeg" : "audio/prs.sid");
		return dt;
	}
}
