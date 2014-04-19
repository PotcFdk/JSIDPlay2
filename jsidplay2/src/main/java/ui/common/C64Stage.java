package ui.common;

import java.util.ArrayList;
import java.util.Collection;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;

public abstract class C64Stage extends Stage implements UIPart {

	protected UIUtil util;

	private Scene scene;
	private boolean wait;

	/** All UI pieces of this Stage */
	private final Collection<UIPart> uiParts = new ArrayList<>();

	public C64Stage(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		util = new UIUtil(this, consolePlayer, player, config, this);
		scene = (Scene) util.parse();
		scene.getStylesheets().add(getStyleSheetName());
	}

	public void open() {
		open(this);
		centerOnScreen();
	}

	public void open(Stage stage) {
		stage.setScene(scene);
		stage.getIcons().add(new Image(util.getBundle().getString("ICON")));
		if (stage.getTitle() == null) {
			stage.setTitle(util.getBundle().getString("TITLE"));
		}
		scene.setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				stage.close();
			}
		});
		stage.setOnCloseRequest((event) -> internalClose());
		if (wait) {
			stage.showAndWait();
		} else {
			stage.show();
		}
	}

	protected void internalClose() {
		for (UIPart part : uiParts) {
			part.doClose();
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

	public Collection<UIPart> getUiParts() {
		return uiParts;
	}

}
