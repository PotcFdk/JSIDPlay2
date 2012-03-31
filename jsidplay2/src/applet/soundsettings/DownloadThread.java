package applet.soundsettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;

import sidplay.ini.IniConfig;

public class DownloadThread extends Thread {
	public static final int MAX_BUFFER_SIZE = 1<<20;

	private boolean isAborted;
	private final IniConfig config;
	private final String fURL;
	private final IDownloadListener fListener;

	public DownloadThread(IniConfig cfg, IDownloadListener listener,
			String url) {
		config = cfg;
		fURL = url;
		fListener = listener;
	}

	@Override
	public void run() {
		try {
			Proxy proxy;
			if (!config.sidplay2().isEnableProxy()) {
				proxy = null;
			} else {
				final SocketAddress addr = new InetSocketAddress(config
						.sidplay2().getProxyHostname(), config.sidplay2()
						.getProxyPort());
				proxy = new Proxy(Proxy.Type.HTTP, addr);
			}


			URL url = new URL(fURL);
			URLConnection conn;
			if (proxy != null) {
				conn = url.openConnection(proxy);
			} else {
				conn = url.openConnection();
			}
			conn.connect();
			InputStream is;
			try {
				is = conn.getInputStream();
			} catch (FileNotFoundException e) {
				fListener.downloadStop(null);
				return;
			}
			File downloadedFile = new File(System.getProperty("jsidplay2.tmpdir"), new File(url.getPath()).getName());
			downloadedFile.deleteOnExit();
			FileOutputStream out = new FileOutputStream(downloadedFile);

			double total = 0;
			int bytesRead;
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while ((bytesRead = is.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				total += bytesRead;
				if (isAborted) {
					fListener.downloadStop(null);
					out.close();
					is.close();
					return;
				}
				fListener
						.downloadStep((int) ((total / conn.getContentLength()) * 100L));
			}
			out.close();
			is.close();
			fListener.downloadStop(downloadedFile);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			fListener.downloadStop(null);
		} catch (IOException e1) {
			e1.printStackTrace();
			fListener.downloadStop(null);
		}

	}

	public void setAborted(boolean aborted) {
		this.isAborted = aborted;
	}
}
