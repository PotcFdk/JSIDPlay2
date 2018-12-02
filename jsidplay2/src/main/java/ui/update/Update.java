package ui.update;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import libsidutils.DesktopIntegration;
import libsidutils.InternetUtils;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.common.C64Window;

public class Update extends C64Window {

	private static final String LOCAL_VERSION_RES = "/META-INF/maven/jsidplay2/jsidplay2/pom.properties";
	private static final String ONLINE_VERSION_RES = "http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/latest.properties?format=raw";

	@FXML
	private TextArea update;
	@FXML
	private Hyperlink latestVersionLink;

	public Update() {
	}

	public Update(Player player) {
		super(player);
	}

	@FXML
	protected void initialize() {
		// check our version
		int[] currentVersion = null;
		try {
			Properties currentProperties = new Properties();
			URL resource = JSidPlay2Main.class.getResource(LOCAL_VERSION_RES);
			currentProperties.load(resource.openConnection().getInputStream());
			currentVersion = getVersionNumbers(currentProperties.getProperty("version"));
		} catch (NullPointerException | IOException e) {
		}
		// check latest version
		int[] latestVersion = null;
		try {
			Properties latestProperties = new Properties();
			Proxy proxy = util.getConfig().getSidplay2Section().getProxy();
			URLConnection connection = InternetUtils.openConnection(new URL(ONLINE_VERSION_RES), proxy);
			latestProperties.load(connection.getInputStream());
			latestVersion = getVersionNumbers(latestProperties.getProperty("version"));
		} catch (NullPointerException | IOException e) {
		}
		final boolean updateAvailable = isUpdateAvailableNewer(currentVersion, latestVersion);
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

	private boolean isUpdateAvailableNewer(int[] currentVersion, int[] latestVersion) {
		if (currentVersion == null || latestVersion == null) {
			// undetermined local or remote version? Do not flag an update
			return false;
		}
		for (int i = 0; i < currentVersion.length; i++)
			if (currentVersion[i] != latestVersion[i]) {
				// current version is outdated
				return currentVersion[i] < latestVersion[i];
			}
		// version is equal
		return false;
	}

	private int[] getVersionNumbers(String ver) {
		Matcher m = Pattern.compile("(\\d+)\\.(\\d+)").matcher(ver);
		if (!m.matches())
			throw new IllegalArgumentException("Malformed FW version");

		return new int[] { Integer.parseInt(m.group(1)), // major
				Integer.parseInt(m.group(2)), // minor
		};
	}

}
