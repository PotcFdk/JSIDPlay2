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
					// JSIDPlay2 components do not use the default constructor
					Constructor<?> constructor = type
							.getConstructor(new Class[] { ConsolePlayer.class,
									Player.class, Configuration.class });
					return (Builder<?>) constructor.newInstance(consolePlayer,
							player, config);
				} catch (NoSuchMethodException | SecurityException
						| InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			return defaultBuilderFactory.getBuilder(type);
		}
	}

	private ConsolePlayer consolePlayer;
	private Player player;
	private Configuration config;

	private ResourceBundle bundle;

	private DoubleProperty progressProperty;

	public UIUtil(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		this.consolePlayer = consolePlayer;
		this.player = player;
		this.config = config;
	}

	public Object parse(final UIPart part) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = part.getFxml();
		fxmlLoader.setLocation(fxml);
		fxmlLoader.setBuilderFactory(new BuilderFactoryImplementation());
		this.bundle = ResourceBundle.getBundle(part.getBundleName());
		fxmlLoader.setResources(this.bundle);
		fxmlLoader.setController(part);
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	void doCloseWindow(Node n) {
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
			progressProperty = progressBar.progressProperty();
		}
		return progressProperty;
	}

	public ConsolePlayer getConsolePlayer() {
		return consolePlayer;
	}

	public Player getPlayer() {
		return player;
	}

	public Configuration getConfig() {
		return config;
	}

	public ResourceBundle getBundle() {
		return bundle;
	}

}
