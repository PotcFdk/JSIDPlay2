package netsiddev;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SIDDeviceUtil {

	public static final Image PLAYED_ICON = new Image(SIDDeviceUtil.class.getResource("jsidplay2.png").toString());

	private ResourceBundle bundle;

	protected Object parse(final SIDDevice part) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = part.getFxml();
		fxmlLoader.setLocation(fxml);
		JavaFXBuilderFactory javaFXBuilderFactory = new JavaFXBuilderFactory();
		fxmlLoader.setBuilderFactory(javaFXBuilderFactory);
		bundle = ResourceBundle.getBundle(part.getBundleName());
		fxmlLoader.setResources(bundle);
		fxmlLoader.setController(part);
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected final void setPlayedGraphics(Node node) {
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

	protected ResourceBundle getBundle() {
		return bundle;
	}

}
