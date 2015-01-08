package ui.about;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import libsidplay.Player;
import ui.JSIDPlay2Main;
import ui.common.C64Window;

public class About extends C64Window {
	private static Properties properties = new Properties();
	static {
		properties.setProperty("version", "(beta)");
		try {
			URL resource = JSIDPlay2Main.class
					.getResource("/META-INF/maven/jsidplay2/jsidplay2/pom.properties");
			properties.load(resource.openConnection().getInputStream());
		} catch (NullPointerException | IOException e) {
		}
	}

	@FXML
	private Text credits;

	public About(Player player) {
		super(player);
		getStage().resizableProperty().set(false);
	}

	@FXML
	private void initialize() {
		credits.setText(util.getPlayer().getCredits(properties));
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

}
