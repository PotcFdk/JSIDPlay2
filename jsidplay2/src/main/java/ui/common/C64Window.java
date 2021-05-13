package ui.common;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import sidplay.Player;

public abstract class C64Window implements UIPart, Initializable {

	protected UIUtil util;

	private Stage stage;
	private Scene scene;
	private boolean wait;

	private Supplier<Boolean> closeActionEnabler = () -> true;

	public C64Window() {
		util = onlyForEclipseJavaFXPreviewView();
	}

	/**
	 * Create a scene in a new stage.
	 */
	public C64Window(Player player) {
		this(new Stage(), player);
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
		stage.setTitle(util.getBundle().getString("TITLE"));
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
			close(getStage().getScene().getRoot());
			doClose();
		}
	}

	public void close(Node node) {
		if (node instanceof UIPart) {
			((UIPart) node).doClose();
		}
		if (node instanceof Parent) {
			((Parent) node).getChildrenUnmodifiable().stream().forEach(child -> close(child));
		}
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

	public UIUtil getUtil() {
		return util;
	}

}
