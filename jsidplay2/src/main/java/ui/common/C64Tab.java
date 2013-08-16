package ui.common;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;
import ui.events.UIEvent;
import ui.events.UIEventFactory;

public abstract class C64Tab extends Tab implements UIPart {

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
	
	@Override
	public void notify(final UIEvent evt) {
	}

	public C64Tab() {
		setContent((Node) util.parse(this));
	}

	protected ResourceBundle getBundle() {
		return util.getBundle();
	}

	protected UIEventFactory getUiEvents() {
		return util.getUiEvents();
	}

	public Player getPlayer() {
		return util.getPlayer();
	}

	public void setPlayer(Player player) {
		util.setPlayer(player);
	}

	public ConsolePlayer getConsolePlayer() {
		return util.getConsolePlayer();
	}

	public void setConsolePlayer(ConsolePlayer consolePlayer) {
		util.setConsolePlayer(consolePlayer);
	}

	public Configuration getConfig() {
		return util.getConfig();
	}

	public void setConfig(Configuration config) {
		util.setConfig(config);
	}

}
