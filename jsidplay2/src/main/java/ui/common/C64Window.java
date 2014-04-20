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

public abstract class C64Window implements UIPart {

	protected UIUtil util;

	private Stage stage;
	private Scene scene;
	private boolean wait;

	/** All UI pieces of this Stage */
	private final Collection<UIPart> uiParts = new ArrayList<>();

	/**
	 * Create a scene in a new stage.
	 */
	public C64Window(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		this(new Stage(), consolePlayer, player, config);
		this.stage.centerOnScreen();
	}

	/**
	 * Create a scene in the existing primary stage.
	 */
	public C64Window(Stage stage, ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		this.stage = stage;
		util = new UIUtil(this, consolePlayer, player, config, this);
		scene = (Scene) util.parse();
		scene.getStylesheets().add(getStyleSheetName());
		scene.setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				close();
			}
		});
		stage.getIcons().add(new Image(util.getBundle().getString("ICON")));
		if (stage.getTitle() == null) {
			stage.setTitle(util.getBundle().getString("TITLE"));
		}
		stage.setOnCloseRequest((event) -> close());
		stage.setScene(scene);
	}

	public void open() {
		if (wait) {
			stage.showAndWait();
		} else {
			stage.show();
		}
	}

	protected void close() {
		stage.close();
		for (UIPart part : uiParts) {
			part.doClose();
		}
	}

	public Stage getStage() {
		return stage;
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

	Collection<UIPart> getUiParts() {
		return uiParts;
	}

}
