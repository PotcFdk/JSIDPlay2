package ui.common;

import java.io.IOException;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;

public abstract class C64Stage extends Stage implements UIPart {

	protected UIUtil util;
	private boolean wait;

	public C64Stage(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		util = new UIUtil(consolePlayer, player, config);
	}

	public void open() throws IOException {
		open(this);
		centerOnScreen();
	}

	public void open(Stage stage) throws IOException {
		Scene scene = (Scene) util.parse(this);
		scene.getStylesheets().add(getStyleSheetName());
		scene.setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				stage.close();
			}
		});
		stage.setScene(scene);
		stage.getIcons().add(new Image(util.getBundle().getString("ICON")));
		stage.setTitle(util.getBundle().getString("TITLE"));
		stage.setOnCloseRequest((event) -> {
			util.doCloseWindow(scene.getRoot());
			doCloseWindow();
		});
		if (wait) {
			stage.showAndWait();
		} else {
			stage.show();
		}
	}

	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

	private String getStyleSheetName() {
		return "/" + getClass().getName().replace('.', '/') + ".css";
	}

}
