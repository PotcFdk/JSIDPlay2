package ui.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.entities.config.Configuration;

public class UIUtil {

	private static final Image PLAYED_ICON = new Image(JSidPlay2Main.class.getResource("icons/play.png").toString());

	private final C64Window window;
	/** Model */
	private final Player player;
	/** View localization */
	private ResourceBundle bundle;
	/** Controller */
	private final UIPart controller;

	/** Progress bar support */
	private DoubleProperty progressProperty;

	public UIUtil(C64Window window, Player player, UIPart controller) {
		this.window = window;
		this.player = player;
		this.controller = controller;
		this.bundle = ResourceBundle.getBundle(controller.getBundleName());

	}

	public Object parse() {
		return parse(null);
	}

	public Object parse(Object root) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = this.controller.getFxml();
		fxmlLoader.setLocation(fxml);
		fxmlLoader.setBuilderFactory(new UIBuilder(this.window, this.player));
		fxmlLoader.setResources(this.bundle);
		fxmlLoader.setController(this.controller);
		if (root != null) {
			fxmlLoader.setRoot(root);
		}
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			throw new RuntimeException("Unparsable View: " + fxml, e);
		}
	}

	private static ObjectProperty<?>[] currentlyPlayedObjectProperty;

	public final void setPlayingTab(Node node, ObjectProperty<?>... newCurrentlyPlayedObjectProperty) {
		resetPlayingTab(node.getScene().getRoot(), currentlyPlayedObjectProperty, newCurrentlyPlayedObjectProperty);
		currentlyPlayedObjectProperty = newCurrentlyPlayedObjectProperty;
		do {
			if (node instanceof UIPart && node.getParent().getParent() instanceof TabPane) {
				TabPane tabPane = (TabPane) node.getParent().getParent();
				final Node thenode = node;
				tabPane.getTabs().stream().filter(tab -> tab.getContent().equals(thenode)).findFirst()
						.ifPresent(tab -> tab.setGraphic(new ImageView(PLAYED_ICON)));
			}
			node = node.getParent();
		} while (node != null);
	}

	private void resetPlayingTab(Node root, ObjectProperty<?>[] oldCurrentlyPlayedObjectProperty,
			ObjectProperty<?>[] newCurrentlyPlayedObjectProperty) {
		if (oldCurrentlyPlayedObjectProperty != null) {
			for (ObjectProperty<?> currently : oldCurrentlyPlayedObjectProperty) {
				if (!Arrays.asList(newCurrentlyPlayedObjectProperty).contains(currently)) {
					currently.set(null);
				}
			}
		}
		root.lookupAll(".tab-pane").forEach(
				tabPaneNode -> ((TabPane) tabPaneNode).getTabs().stream().forEach(tab -> tab.setGraphic(null)));
	}

	public final DoubleProperty progressProperty(Scene scene) {
		if (progressProperty == null && scene != null) {
			ProgressBar progressBar = (ProgressBar) scene.lookup("#progress");
			if (progressBar != null) {
				progressProperty = progressBar.progressProperty();
			}
		}
		return progressProperty;
	}

	public C64Window getWindow() {
		return window;
	}

	public final Player getPlayer() {
		return player;
	}

	public final Configuration getConfig() {
		return (Configuration) player.getConfig();
	}

	public final ResourceBundle getBundle() {
		return bundle;
	}

	void setBundle(ResourceBundle bundle) {
		this.bundle = bundle;
	}
}
