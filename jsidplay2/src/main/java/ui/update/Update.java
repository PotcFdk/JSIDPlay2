package ui.update;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import libsidplay.Player;
import ui.JSIDPlay2Main;
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
			URL resource = JSIDPlay2Main.class
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
		// Open a browser URL

		// As an application we open the default browser
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				try {
					desktop.browse(new URL(
							"http://sourceforge.net/projects/jsidplay2/files/latest/download")
							.toURI());
				} catch (final IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("Awt Desktop is not supported!");
		}
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

}
