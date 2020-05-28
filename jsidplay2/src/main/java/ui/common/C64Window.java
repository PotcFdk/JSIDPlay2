package ui.common;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sidplay.Player;
import ui.entities.config.Configuration;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public abstract class C64Window implements UIPart, Initializable {

	protected UIUtil util;

	private Stage stage;
	private Scene scene;
	private boolean wait;

	private Supplier<Boolean> closeActionEnabler = () -> true;

	/**
	 * Default Constructor for JavaFX Preview in Eclipse, only (Player with default
	 * configuration for the controller)
	 */
	public C64Window() {
		ConfigService configService = new ConfigService(ConfigurationType.XML);
		Configuration configuration = configService.load();
		util = new UIUtil(null, new Player(configuration), this);
		configService.close();
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
		stage.initStyle(StageStyle.UTILITY);
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
