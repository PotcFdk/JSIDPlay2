package ui.common;

import java.io.IOException;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

	public C64Stage(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		util = new UIUtil(consolePlayer, player, config, this);
		scene = (Scene) util.parse();
		scene.getStylesheets().add(getStyleSheetName());
	}

	public void open() throws IOException {
		open(this);
		centerOnScreen();
	}

	public void open(Stage stage) throws IOException {
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
		stage.setOnCloseRequest((event) -> {
			doCloseWindow(scene.getRoot());
			doCloseWindow();
		});
		if (wait) {
			stage.showAndWait();
		} else {
			stage.show();
		}
	}

	private void doCloseWindow(Node n) {
		if (n instanceof TabPane) {
			TabPane theTabPane = (TabPane) n;
			for (Tab tab : theTabPane.getTabs()) {
				if (tab instanceof UIPart) {
					UIPart theTab = (UIPart) tab;
					theTab.doCloseWindow();
				}
			}
		}
		if (n instanceof UIPart) {
			UIPart theTab = (UIPart) n;
			theTab.doCloseWindow();
		}
		if (n instanceof Parent) {
			for (Node c : ((Parent) n).getChildrenUnmodifiable()) {
				doCloseWindow(c);
			}
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
