package ui.common;

import java.net.URL;

import javafx.util.Builder;

public interface UIPart extends Builder<Object> {

	default String getBundleName() {
		return getClass().getName();
	}

	default URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	default Object build() {
		return this;
	}

	default void doCloseWindow() {
	}

}
