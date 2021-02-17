package ui.common.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import ui.JSidPlay2Main;
import ui.entities.config.SidPlay2Section;

public class VersionUtil {

	private static final String ONLINE_VERSION_RES = "http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/latest.properties?format=raw";

	private static Properties properties = new Properties();

	public static void init() {
		properties.setProperty("version", "(beta)");
		try {
			URL resource = JSidPlay2Main.class.getResource("/META-INF/maven/jsidplay2/jsidplay2/pom.properties");
			properties.load(resource.openConnection().getInputStream());
		} catch (NullPointerException | IOException e) {
		}
	}

	public static String getVersion() {
		return properties.getProperty("version");
	}

	public static String getRemoteVersion(SidPlay2Section sidplay2Section) {
		try {
			Properties latestProperties = new Properties();
			URLConnection connection = InternetUtil.openConnection(new URL(ONLINE_VERSION_RES), sidplay2Section);
			latestProperties.load(connection.getInputStream());
			return latestProperties.getProperty("version");
		} catch (NullPointerException | IOException e) {
			return null;
		}

	}
}
