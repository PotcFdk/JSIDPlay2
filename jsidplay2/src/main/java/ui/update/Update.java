package ui.update;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import libsidutils.DesktopIntegration;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.common.C64Window;

public class Update extends C64Window {

	@FXML
	private TextArea update;
	@FXML
	private Hyperlink latestVersionLink;

	public Update(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		// check our version
		float currentVersion = Integer.MAX_VALUE;
		try {
			Properties currentProperties = new Properties();
			URL resource = JSidPlay2Main.class.getResource("/META-INF/maven/jsidplay2/jsidplay2/pom.properties");
			currentProperties.load(resource.openConnection().getInputStream());
			currentVersion = Float.parseFloat(currentProperties.getProperty("version"));
		} catch (NullPointerException | IOException e) {
		}
		// check latest version
		float latestVersion = Integer.MIN_VALUE;
		try {
			Properties latestProperties = new Properties();
			URL url = new URL(
					"http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/latest.properties?format=raw");
			while (true) {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection(getProxy());
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
						URL next = new URL(url, location);
						url = new URL(next.toExternalForm());
						continue;
					}
				}
				latestProperties.load(connection.getInputStream());
				latestVersion = Float.parseFloat(latestProperties.getProperty("version"));
				break;
			}
		} catch (NullPointerException | IOException e) {
		}
		final boolean updateAvailable = latestVersion > currentVersion;
		latestVersionLink.setVisible(updateAvailable);
		update.setText(util.getBundle().getString(updateAvailable ? "UPDATE_AVAILABLE" : "NO_UPDATE"));
	}

	@FXML
	private void gotoLatestVersion() {
		DesktopIntegration.browse("http://sourceforge.net/projects/jsidplay2/files/latest/download");
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

	private Proxy getProxy() {
		if (util.getConfig().getSidplay2Section().isEnableProxy()) {
			final SocketAddress addr = new InetSocketAddress(util.getConfig().getSidplay2Section().getProxyHostname(),
					util.getConfig().getSidplay2Section().getProxyPort());
			return new Proxy(Proxy.Type.HTTP, addr);
		} else {
			return Proxy.NO_PROXY;
		}
	}

}
