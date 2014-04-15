package ui.common;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.JSIDPlay2Main;
import ui.entities.config.Configuration;

public class UIUtil {

	private static final Image PLAYED_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/play.png").toString());

	private final class BuilderFactoryImplementation implements BuilderFactory {
		private JavaFXBuilderFactory defaultBuilderFactory = new JavaFXBuilderFactory();

		@Override
		public Builder<?> getBuilder(Class<?> type) {
			if (UIPart.class.isAssignableFrom(type)) {
				try {
					Constructor<?> constructor = type
							.getConstructor(new Class[] { ConsolePlayer.class,
									Player.class, Configuration.class });
					return (Builder<?>) constructor.newInstance(consolePlayer,
							player, config);
				} catch (NoSuchMethodException | SecurityException
						| InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(
							"UIPart implementations needs a 3-arg constructor to provide the model");
				}
			}
			return defaultBuilderFactory.getBuilder(type);
		}
	}

	/** Model **/
	private ConsolePlayer consolePlayer;
	private Player player;
	private Configuration config;
	/** View */
	private ResourceBundle bundle;
	/** Controller */
	private UIPart controller;

	private DoubleProperty progressProperty;

	public UIUtil(ConsolePlayer consolePlayer, Player player,
			Configuration config, UIPart controller) {
		this.consolePlayer = consolePlayer;
		this.player = player;
		this.config = config;
		this.controller = controller;
		this.bundle = ResourceBundle.getBundle(controller.getBundleName());
	}

	public Object parse() {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = this.controller.getFxml();
		fxmlLoader.setLocation(fxml);
		fxmlLoader.setBuilderFactory(new BuilderFactoryImplementation());
		fxmlLoader.setResources(this.bundle);
		fxmlLoader.setController(this.controller);
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unparsable View: " + fxml);
		}
	}

	public final void setPlayedGraphics(Node node) {
		Parent p = node.getParent();
		while (p != null) {
			if (p instanceof TabPane) {
				TabPane tabPane = (TabPane) p;
				for (Tab tab : tabPane.getTabs()) {
					tab.setGraphic(null);
				}
				tabPane.getSelectionModel().selectedItemProperty().get()
						.setGraphic(new ImageView(PLAYED_ICON));
			}
			p = p.getParent();
		}
	}

	public final DoubleProperty progressProperty(Node node) {
		if (progressProperty == null) {
			ProgressBar progressBar = (ProgressBar) node.getScene().lookup(
					"#progress");
			if (progressBar == null) {
				throw new RuntimeException("Progress Bar does not exist!");
			}
			progressProperty = progressBar.progressProperty();
		}
		return progressProperty;
	}

	public final ConsolePlayer getConsolePlayer() {
		return consolePlayer;
	}

	public final Player getPlayer() {
		return player;
	}

	public final Configuration getConfig() {
		return config;
	}

	public final ResourceBundle getBundle() {
		return bundle;
	}

}
