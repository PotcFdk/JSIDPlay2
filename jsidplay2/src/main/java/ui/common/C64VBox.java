package ui.common;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import sidplay.Player;
import ui.entities.config.Configuration;

public abstract class C64VBox extends VBox implements UIPart, Initializable {

	protected UIUtil util;

	/**
	 * Default Constructor for JavaFX Preview in Eclipse, only (Player with default
	 * configuration for the controller)
	 */
	public C64VBox() {
		util = new UIUtil(null, new Player(new Configuration()), this);
		// XXX Uncomment line ONLY for JavaFX Preview in Eclipse of JSidPlay2.fxml -->
//		util.parse(this);
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
