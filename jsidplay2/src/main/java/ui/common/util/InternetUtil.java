package ui.common.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import ui.entities.config.SidPlay2Section;

public class InternetUtil {

	public static URLConnection openConnection(URL currentURL, SidPlay2Section sidplay2Section) throws IOException {
		while (true) {
			URLConnection openConnection = currentURL.openConnection(getProxy(sidplay2Section));
			if (openConnection instanceof HttpURLConnection) {
				HttpURLConnection connection = (HttpURLConnection) openConnection;
				connection.setInstanceFollowRedirects(false);
				int responseCode = connection.getResponseCode();
				switch (responseCode) {
				case HttpURLConnection.HTTP_MOVED_PERM:
				case HttpURLConnection.HTTP_MOVED_TEMP:
				case HttpURLConnection.HTTP_SEE_OTHER:
					String location = connection.getHeaderField("Location");
					if (location != null) {
						location = URLDecoder.decode(location, UTF_8.name());
						// Deal with relative URLs
						URL next = new URL(currentURL, location);
						currentURL = new URL(next.toExternalForm().replace("%", "%25").replace(" ", "%20"));
						continue;
					}
				case HttpURLConnection.HTTP_OK:
					break;
				default:
					throw new IOException("Unexpected response: " + responseCode);
				}
			}
			return openConnection;
		}
	}

	private static Proxy getProxy(SidPlay2Section sidplay2Section) {
		if (sidplay2Section.isEnableProxy()) {
			return new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(sidplay2Section.getProxyHostname(), sidplay2Section.getProxyPort()));
		} else {
			return Proxy.NO_PROXY;
		}
	}

}
