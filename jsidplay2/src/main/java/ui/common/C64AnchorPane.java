package ui.common;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;

public abstract class C64AnchorPane extends AnchorPane implements UIPart {

	private UIUtil util = new UIUtil();

	@Override
	public String getBundleName() {
		return getClass().getName();
	}

	@Override
	public URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}
	
	public C64AnchorPane() {
		getChildren().add((Node) util.parse(this));
	}

	protected ResourceBundle getBundle() {
		return util.getBundle();
	}

	public Player getPlayer() {
		return util.getPlayer();
	}

	public void setPlayer(Player player) {
		util.setPlayer(player);
	}

	public Configuration getConfig() {
		return util.getConfig();
	}

	public void setConfig(Configuration config) {
		util.setConfig(config);
	}

	public ConsolePlayer getConsolePlayer() {
		return util.getConsolePlayer();
	}

	public void setConsolePlayer(ConsolePlayer cp) {
		util.setConsolePlayer(cp);
	}

}
