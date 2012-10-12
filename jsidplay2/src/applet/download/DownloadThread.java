package applet.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import sidplay.ini.intf.IConfig;

public class DownloadThread extends Thread implements RBCWrapperDelegate {
	public static final int MAX_BUFFER_SIZE = 1 << 20;

	private final IConfig config;
	private final String fURL;
	private final IDownloadListener fListener;

	public DownloadThread(IConfig cfg, IDownloadListener listener, String url) {
		config = cfg;
		fURL = url;
		fListener = listener;
	}

	@Override
	public void run() {
		Proxy proxy = null;
		if (config.getSidplay2().isEnableProxy()) {
			final SocketAddress addr = new InetSocketAddress(config
					.getSidplay2().getProxyHostname(), config.getSidplay2()
					.getProxyPort());
			proxy = new Proxy(Proxy.Type.HTTP, addr);
		}

		File downloadedFile;
		FileOutputStream fos = null;
		try {
			URL url = new URL(fURL);
			int contentLength = getContentLength(proxy, url);
			downloadedFile = new File(System.getProperty("jsidplay2.tmpdir"),
					new File(url.getPath()).getName());
			downloadedFile.deleteOnExit();

			final URLConnection openConnection = openConnection(proxy, url);
			ReadableByteChannel rbc = new RBCWrapper(
					Channels.newChannel(openConnection.getInputStream()),
					contentLength, this);
			fos = new FileOutputStream(downloadedFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch (IOException e) {
			e.printStackTrace();
			fListener.downloadStop(null);
			return;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		fListener.downloadStop(downloadedFile);

	}

	private URLConnection openConnection(Proxy proxy, URL url)
			throws IOException {
		final URLConnection openConnection;
		if (proxy != null) {
			openConnection = url.openConnection(proxy);
		} else {
			openConnection = url.openConnection();
		}
		return openConnection;
	}

	@Override
	public void rbcProgressCallback(RBCWrapper rbc, double progress) {
		fListener.downloadStep((int) (rbc.getReadSoFar() / (rbc
				.getExpectedSize() / 100)));
	}

	private int getContentLength(Proxy proxy, URL url) {
		HttpURLConnection connection;
		int contentLength = -1;

		try {
			HttpURLConnection.setFollowRedirects(false);

			connection = (HttpURLConnection) openConnection(proxy, url);
			connection.setRequestMethod("HEAD");

			contentLength = connection.getContentLength();
		} catch (Exception e) {
		}

		return contentLength;
	}
}
