package ui.common;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;
import ui.events.UIEventFactory;

public abstract class C64Stage extends Stage implements UIPart {

	private UIUtil util = new UIUtil();

	public void open() throws IOException {
		open(this);
		centerOnScreen();
	}

	public void open(Stage stage) throws IOException {
		Scene scene = (Scene) util.parse(this);
		scene.getStylesheets().add(getStyleSheetName());
		stage.setScene(scene);
		stage.getIcons().add(new Image(getBundle().getString("ICON")));
		stage.setTitle(getBundle().getString("TITLE"));
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				getUiEvents().removeListener(C64Stage.this);
				doCloseWindow();
			}
		});
		stage.show();
	}

	protected ResourceBundle getBundle() {
		return util.getBundle();
	}

	protected UIEventFactory getUiEvents() {
		return util.getUiEvents();
	}

	public Configuration getConfig() {
		return util.getConfig();
	}

	public void setConfig(Configuration config) {
		util.setConfig(config);
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

	protected abstract String getStyleSheetName();

	protected abstract void doCloseWindow();

}
