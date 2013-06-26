package ui.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;

public class UIUtil extends UIC64 {

	private ResourceBundle bundle;

	protected Object parse(final UIPart part) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = part.getFxml();
		fxmlLoader.setLocation(fxml);
		JavaFXBuilderFactory javaFXBuilderFactory = new JavaFXBuilderFactory();
		fxmlLoader.setBuilderFactory(javaFXBuilderFactory);
		bundle = ResourceBundle.getBundle(part.getBundleName());
		fxmlLoader.setResources(bundle);
		getUiEvents().addListener(part);
		fxmlLoader.setController(part);
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected ResourceBundle getBundle() {
		return bundle;
	}

}
