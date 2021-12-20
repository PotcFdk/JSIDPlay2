package ui.common.util;

import static ui.common.util.InternetUtil.openConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import ui.entities.config.SidPlay2Section;

public class VersionUtil {

	private static final String LOCAL_VERSION_RESOURCE = "/META-INF/maven/jsidplay2/jsidplay2/pom.properties";

	private static final String ONLINE_VERSION_RESOURCE = "http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/latest.properties?format=raw";

	public static String VERSION;

	static {
		try {
			URLConnection connection = VersionUtil.class.getResource(LOCAL_VERSION_RESOURCE).openConnection();
			VERSION = getVersion(connection);
		} catch (Exception e) {
			VERSION = "(beta)";
		}
	}

	public static String fetchRemoteVersion(SidPlay2Section sidplay2Section) {
		try {
			URLConnection connection = openConnection(new URL(ONLINE_VERSION_RESOURCE), sidplay2Section);
			return getVersion(connection);
		} catch (Exception e) {
			return null;
		}
	}

	private static String getVersion(URLConnection connection) throws IOException {
		try (InputStream inputStream = connection.getInputStream()) {
			Properties latestProperties = new Properties();
			latestProperties.load(inputStream);
			return latestProperties.getProperty("version");
		}
	}
}
