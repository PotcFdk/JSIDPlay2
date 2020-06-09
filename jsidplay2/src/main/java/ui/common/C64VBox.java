package ui.common;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import sidplay.Player;
import ui.entities.config.Configuration;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public abstract class C64VBox extends VBox implements UIPart, Initializable {

	protected UIUtil util;

	/**
	 * Default Constructor for JavaFX Preview in Eclipse, only (Player with default
	 * configuration for the controller)
	 */
	public C64VBox() {
		ConfigService configService = new ConfigService(ConfigurationType.XML);
		Configuration configuration = configService.load();
		util = new UIUtil(null, new Player(configuration), this);
		configService.close();
		util.parse(this);
	}

	public C64VBox(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		util.parse(this);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		util.setBundle(resources);
		initialize();
	}

	protected abstract void initialize();

}
