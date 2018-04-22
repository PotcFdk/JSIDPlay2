package ui.common;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import sidplay.Player;
import ui.entities.config.Configuration;

public abstract class C64Window implements UIPart, Initializable {

	protected UIUtil util;

	private Stage stage;
	private Scene scene;
	private boolean wait;

	/** All UI pieces of this Stage */
	private final Collection<UIPart> uiParts = new ArrayList<>();

	private Supplier<Boolean> closeActionEnabler = () -> true;

	/**
	 * Default Constructor for JavaFX Preview in Eclipse, only (Player with default
	 * configuration for the controller)
	 */
	public C64Window() {
		util = new UIUtil(null, new Player(new Configuration()), this);
	}
	
	/**
	 * Create a scene in a new stage.
	 */
	public C64Window(Player player) {
		this(new Stage(), player);
		Platform.runLater(() -> this.stage.centerOnScreen());
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		util.setBundle(resources);
		initialize();
	}

	protected abstract void initialize();

	/**
	 * Create a scene in the existing primary stage.
	 */
	public C64Window(Stage stage, Player player) {
		this.stage = stage;
		util = new UIUtil(this, player, this);
		scene = (Scene) util.parse();
		scene.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				close();
			}
		});
		stage.getIcons().add(new Image(util.getBundle().getString("ICON")));
		if (stage.getTitle() == null) {
			stage.setTitle(util.getBundle().getString("TITLE"));
		}
		stage.setOnCloseRequest(event -> close());
		stage.setScene(scene);
	}

	public void open() {
		if (wait) {
			stage.showAndWait();
		} else {
			stage.show();
		}
	}

	public void close() {
		if (closeActionEnabler.get()) {
			stage.close();
			for (UIPart part : uiParts) {
				part.doClose();
			}
		}
	}

	public void close(UIPart part) {
		part.doClose();
		uiParts.remove(part);
	}

	public void setCloseActionEnabler(Supplier<Boolean> closeActionEnabler) {
		this.closeActionEnabler = closeActionEnabler;
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
