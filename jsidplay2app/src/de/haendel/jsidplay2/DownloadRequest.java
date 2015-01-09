package de.haendel.jsidplay2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;

import android.net.Uri;
import android.os.Environment;

public class DownloadRequest extends LongRunningRequest<DataAndType> {
	private static final String DOWNLOAD_DIR = "Download";

	public DownloadRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
	}

	@Override
	protected DataAndType getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		InputStream content = httpEntity.getContent();
		File sdRootDir = Environment.getExternalStorageDirectory();
		File music = new File(new File(sdRootDir, DOWNLOAD_DIR),
				new File(url).getName());

		OutputStream out;
		byte[] b = new byte[4096];
		if (sdRootDir.canWrite()) {
			out = new BufferedOutputStream(new FileOutputStream(music));
			while (content.available() > 0) {
				int n = content.read(b);
				out.write(b, 0, n);
			}
			out.close();
		}
		DataAndType dt = new DataAndType();
		dt.uri = Uri.fromFile(music);
		dt.type = url.endsWith(".mp3") ? "audio/mpeg" : "audio/prs.sid";
		return dt;
	}
}
