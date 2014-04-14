package netsiddev;

import java.net.URL;

public interface SIDDeviceUIPart {

	default String getBundleName() {
		return getClass().getName();
	}

	default URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

}
