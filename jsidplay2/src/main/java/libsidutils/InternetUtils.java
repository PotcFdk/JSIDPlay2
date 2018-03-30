package libsidutils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

public class InternetUtils {

	public static URLConnection openConnection(URL currentURL, Proxy proxy) throws IOException {
		while (true) {
			URLConnection openConnection = currentURL.openConnection(proxy);
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
						location = URLDecoder.decode(location, "UTF-8");
						// Deal with relative URLs
						URL next = new URL(currentURL, location);
						currentURL = new URL(next.toExternalForm().replace(" ", "%20"));
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

}
