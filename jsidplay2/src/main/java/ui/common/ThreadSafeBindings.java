package ui.common;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;

public class ThreadSafeBindings {

	public static <U> void bindBidirectional(ObjectProperty<U> fxProperty, ObjectProperty<U> configProperty) {
		fxProperty.addListener((obj, o, n) -> configProperty.setValue(fxProperty.get()));
		configProperty.addListener((obj, o, n) -> Platform.runLater(() -> fxProperty.setValue(n)));
		fxProperty.setValue(configProperty.get());
	}

}
