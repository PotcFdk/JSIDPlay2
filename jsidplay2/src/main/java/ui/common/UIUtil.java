package ui.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.entities.config.Configuration;

public class UIUtil {

	private static final Image PLAYED_ICON = new Image(JSidPlay2Main.class
			.getResource("icons/play.png").toString());

	private C64Window window;
	/** Model */
	private Player player;
	/** View localization */
	private ResourceBundle bundle;
	/** Controller */
	private UIPart controller;

	/** Progress bar support */
	private DoubleProperty progressProperty;

	public UIUtil(C64Window window, Player player, UIPart controller) {
		this.window = window;
		this.player = player;
		this.controller = controller;
		this.bundle = ResourceBundle.getBundle(controller.getBundleName());
		window.getUiParts().add(controller);
	}

	public Object parse() {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = this.controller.getFxml();
		fxmlLoader.setLocation(fxml);
		fxmlLoader.setBuilderFactory(new UIBuilder(this.window, this.player));
		fxmlLoader.setResources(this.bundle);
		fxmlLoader.setController(this.controller);
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unparsable View: " + fxml);
		}
	}

	public final void setPlayingTab(Tab tab) {
		resetPlayingTab(tab.getTabPane().getScene().getRoot());
		tab.setGraphic(new ImageView(PLAYED_ICON));
		Parent p = tab.getTabPane().getParent();
		while (p != null) {
			if (p instanceof TabPane) {
				TabPane tabPane = (TabPane) p;
				tabPane.getSelectionModel().selectedItemProperty().get()
						.setGraphic(new ImageView(PLAYED_ICON));
			}
			p = p.getParent();
		}
	}

	private void resetPlayingTab(Node n) {
		if (n instanceof TabPane) {
			TabPane tabPane = (TabPane) n;
			for (Tab tab : tabPane.getTabs()) {
				tab.setGraphic(null);
			}
		}
		if (n instanceof Parent) {
			for (Node c : ((Parent) n).getChildrenUnmodifiable()) {
				resetPlayingTab(c);
			}
		}
	}

	public final DoubleProperty progressProperty(Node node) {
		if (progressProperty == null && node.getScene() != null) {
			ProgressBar progressBar = (ProgressBar) node.getScene().lookup(
					"#progress");
			if (progressBar != null) {
				progressProperty = progressBar.progressProperty();
			}
		}
		return progressProperty;
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

}
