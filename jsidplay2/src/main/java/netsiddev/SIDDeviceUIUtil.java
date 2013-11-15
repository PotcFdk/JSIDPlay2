package netsiddev;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;

public class SIDDeviceUIUtil {

	private ResourceBundle bundle;

	protected Object parse(final SIDDeviceUIPart part) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		URL fxml = part.getFxml();
		fxmlLoader.setLocation(fxml);
		fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
		bundle = ResourceBundle.getBundle(part.getBundleName());
		fxmlLoader.setResources(bundle);
		fxmlLoader.setController(part);
		try (InputStream is = fxml.openStream()) {
			return fxmlLoader.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected ResourceBundle getBundle() {
		return bundle;
	}

}
