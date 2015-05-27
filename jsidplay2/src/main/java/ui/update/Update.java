package ui.update;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import libsidplay.Player;
import libsidutils.WebUtils;
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
			URL resource = JSidPlay2Main.class
					.getResource("/META-INF/maven/jsidplay2/jsidplay2/pom.properties");
			currentProperties.load(resource.openConnection().getInputStream());
			currentVersion = Float.parseFloat(currentProperties
					.getProperty("version"));
		} catch (NullPointerException | IOException e) {
		}
		// check latest version
		float latestVersion = Integer.MAX_VALUE;
		try {
			Properties latestProperties = new Properties();
			URL resource = new URL(
					"http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/latest.properties?format=raw");
			latestProperties.load(resource.openConnection().getInputStream());
			latestVersion = Float.parseFloat(latestProperties
					.getProperty("version"));
		} catch (NullPointerException | IOException e) {
		}
		final boolean updateAvailable = latestVersion > currentVersion;
		latestVersionLink.setVisible(updateAvailable);
		update.setText(util.getBundle().getString(
				updateAvailable ? "UPDATE_AVAILABLE" : "NO_UPDATE"));
	}

	@FXML
	private void gotoLatestVersion() {
		WebUtils.browse("http://sourceforge.net/projects/jsidplay2/files/latest/download");
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

}
