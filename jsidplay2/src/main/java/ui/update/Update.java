package ui.update;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import libsidutils.DesktopIntegration;
import libsidutils.InternetUtils;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.common.C64Window;
import ui.entities.config.SidPlay2Section;

public class Update extends C64Window {

	private static final String LOCAL_VERSION_RES = "/META-INF/maven/jsidplay2/jsidplay2/pom.properties";
	private static final String ONLINE_VERSION_RES = "http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/latest.properties?format=raw";

	@FXML
	private TextArea update;
	@FXML
	private Hyperlink latestVersionLink;

	public Update() {
		super();
	}

	public Update(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

		update.setText(util.getBundle().getString("PLEASE_WAIT"));
		PauseTransition pauseTransition = new PauseTransition(Duration.millis(1000));
		SequentialTransition sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {
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
				URLConnection connection = InternetUtils.openConnection(new URL(ONLINE_VERSION_RES), sidplay2Section);
				latestProperties.load(connection.getInputStream());
				latestVersion = getVersionNumbers(latestProperties.getProperty("version"));
			} catch (NullPointerException | IOException e) {
			}
			final boolean updateAvailable = isUpdateAvailableNewer(currentVersion, latestVersion);
			latestVersionLink.setVisible(updateAvailable);
			update.setText(util.getBundle().getString(updateAvailable ? "UPDATE_AVAILABLE" : "NO_UPDATE"));
		});
		sequentialTransition.playFromStart();
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
		for (int i = 0; i < currentVersion.length; i++) {
			if (currentVersion[i] != latestVersion[i]) {
				// current version is outdated
				return currentVersion[i] < latestVersion[i];
			}
		}
		// version is equal
		return false;
	}

	private int[] getVersionNumbers(String ver) {
		Matcher m = Pattern.compile("(\\d+)\\.(\\d+)").matcher(ver);
		if (!m.matches()) {
			throw new IllegalArgumentException("Malformed FW version");
		}

		return new int[] { Integer.parseInt(m.group(1)), // major
				Integer.parseInt(m.group(2)), // minor
		};
	}

}
