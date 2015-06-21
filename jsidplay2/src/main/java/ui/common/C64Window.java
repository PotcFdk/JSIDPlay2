package ui.common;

import java.util.ArrayList;
import java.util.Collection;

import sidplay.Player;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

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
	public C64Window(Player player) {
		this(new Stage(), player);
		this.stage.centerOnScreen();
	}

	/**
	 * Create a scene in the existing primary stage.
	 */
	public C64Window(Stage stage, Player player) {
		this.stage = stage;
		util = new UIUtil(this, player, this);
		scene = (Scene) util.parse();
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

	public void close(UIPart part) {
		part.doClose();
		uiParts.remove(part);
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

	Collection<UIPart> getUiParts() {
		return uiParts;
	}

	public UIUtil getUtil() {
		return util;
	}
}
