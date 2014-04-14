package ui;

import java.net.URL;

public interface UIPart {

	default String getBundleName() {
		return getClass().getName();
	}

	default URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

}
